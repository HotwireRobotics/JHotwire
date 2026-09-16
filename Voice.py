"""
Voice transcriber.

Listens on the default microphone, matches speech against a fixed grammar, and
publishes the recognized command to NetworkTables for frc.robot.hotwire.Voice to
dispatch. Recognition is offline; nothing here needs the venue network.

Commands are spoken as "Helix <command>", for example "Helix run intake ten
seconds". Only "test" and "stop" are heard without the wake word. Durations are
any whole number of seconds up to sixty, including compounds like "forty five".

Utterances are published as "<nonce> <verb> <seconds>", with the seconds field
dropped when the phrase carries no duration. The nonce lets the same phrase fire
twice in a row.

Helix answers aloud as the command goes out, and refuses out loud what the robot
cannot act on -- a command spoken to a disabled robot, or to none at all. Every
answer is drawn from a pool, so the same command twice does not get the same
line back. He also answers being spoken to: "Helix, how are you", "Helix,
status", "Helix, what can you do". Those dispatch nothing and are answered with
no robot present.

The reply is synthesized on its own thread, so the microphone keeps hearing
while Helix talks; pass --half-duplex to deafen it for the length of a reply.

The robot is found rather than named: a simulator on this machine answers on the
loopback address, and the roboRIO on the team's, so the same command works in
the shop and on the field. It keeps looking while the link is down, which covers
a simulator started after this.

    pip install pyntcore sounddevice vosk pyttsx3
    python Voice.py [--team 2990] [--server 10.29.90.2] [--model model]

The Vosk model is a directory, downloaded separately from
https://alphacephei.com/vosk/models -- vosk-model-small-en-us is enough for a
grammar this size.
"""

import argparse
import json
import queue
import random
import socket
import sys
import threading
import time

import sounddevice as sd
from ntcore import NetworkTableInstance
from vosk import KaldiRecognizer, Model, SetLogLevel

# Speech is optional: without it Helix still hears and dispatches, silently.
try:
    import pyttsx3
except ImportError:
    pyttsx3 = None

ENTRY = "/SmartDashboard/Voice/Utterance"
RATE = 16000

# Where the robot might be. A simulator serves NT on this machine, so anything
# listening on the loopback port is one; otherwise the roboRIO is at its static
# address. The two are checked in that order, and again whenever the link is
# down for this long, so starting a simulator later still finds it.
SIM = "127.0.0.1"
PORT = 5810
RELOOK = 5.0

# Control word the driver station publishes. Bit zero is set while the robot is
# enabled, which is what decides whether a command can run at all.
CONTROL = "/FMSInfo/FMSControlData"
ENABLED = 1

# Wake word every command needs, so stray conversation cannot drive the robot.
WAKE = "helix"

# Verbs allowed without the wake word: a diagnostic, and an all-stop that has to
# work the instant it is said.
BARE = {"test", "stop"}

# Spoken phrase to the verb the robot binds. Longest phrases match first, so
# "extend intake" is read as "extend" rather than as the "intake" verb.
VERBS = {
    "run intake": "intake",
    "run hopper": "hopper",
    "run shooter": "shooter",
    "extend intake": "extend",
    "retract intake": "retract",
    "extend": "extend",
    "retract": "retract",
    "test": "test",
    "stop": "stop",
}

# Spoken durations, combined into any whole count of seconds in [1, 60]. Kept as
# a closed vocabulary because recognition over one is far more reliable than
# free-form dictation. "for" is deliberately absent: it is a homophone of "four"
# and would be heard as a duration.
UNITS = {
    "one": 1, "two": 2, "three": 3, "four": 4, "five": 5,
    "six": 6, "seven": 7, "eight": 8, "nine": 9,
}
TEENS = {
    "ten": 10, "eleven": 11, "twelve": 12, "thirteen": 13, "fourteen": 14,
    "fifteen": 15, "sixteen": 16, "seventeen": 17, "eighteen": 18,
    "nineteen": 19,
}
TENS = {"twenty": 20, "thirty": 30, "forty": 40, "fifty": 50, "sixty": 60}

# Verbs the robot honors while disabled, mirroring the bindings marked
# ignoringDisable in RobotContainer.configureVoiceBindings(). Anything else
# spoken to a disabled robot would be dropped by the scheduler, so Helix says so
# rather than publishing into the dark.
DISABLED_OK = {"test", "stop"}

# Verbs that run for a while, and so carry a spoken duration. The rest are
# momentary, and Helix answers them without one.
TIMED = {"intake", "hopper", "shooter"}

# Phrases Helix answers and does not dispatch. They move nothing, so they are
# heard whatever state the robot is in, or whether there is one at all. The
# wake word is still required: these are things said to Helix, not near it.
TALK = {
    "how are you": "how",
    "are you there": "here",
    "are you ready": "status",
    "status": "status",
    "report": "status",
    "hello": "hello",
    "good morning": "hello",
    "thank you": "thanks",
    "thanks": "thanks",
    "who are you": "who",
    "what can you do": "help",
}

# Everything Helix listens for, longest first so "extend intake" is read as
# "extend" rather than as the "intake" verb.
PHRASES = {**VERBS, **TALK}
SPOKEN_ONLY = set(TALK.values())

# Words allowed inside a phrase that carry nothing, dropped before matching, so
# "helix please run the intake" is the same command as "helix run intake".
FILLER = {"please", "the", "doing", "today", "now"}


class Bag:
    """Draws lines from a pool, shuffled, and does not repeat one back to back.

    A command given twice gets two different answers, and every line in a pool
    is heard once before any of them comes round again.
    """

    def __init__(self, lines):
        self.lines = tuple(lines)
        self.left = []
        self.last = None

    def draw(self):
        if not self.left:
            self.left = list(self.lines)
            random.shuffle(self.left)

            # A refill must not open on the line the last pass closed with.
            if len(self.left) > 1 and self.left[-1] == self.last:
                self.left.insert(0, self.left.pop())

        self.last = self.left.pop()
        return self.last


# What Helix says as a command goes out. Clauses, which reply() closes with the
# duration when one was spoken.
ACKS = {verb: Bag(pool) for verb, pool in {
    "intake": ("The intake is running", "Intake engaged", "Running the intake",
               "Intake underway"),
    "hopper": ("The hopper is running", "Hopper engaged", "Running the hopper",
               "Hopper underway"),
    "shooter": ("The shooter is spinning up", "Spinning up the shooter",
                "Shooter engaged", "Bringing the shooter to speed"),
    "extend": ("The intake is extended", "Intake deployed", "Extending the intake",
               "It is done"),
    "retract": ("The intake is retracted", "Intake stowed", "Retracting the intake",
                "It is done"),
    "test": ("Diagnostic acknowledged", "Diagnostic flag toggled", "Signal received",
             "Checking in"),
    "stop": ("All motion halted", "Everything is held", "Halting everything",
             "All operations cancelled"),
}.items()}

# The clause for a verb with no pool of its own.
SENT = Bag(("Operation sent", "Command away", "On its way"))

# Whole lines. "done" closes a run whose length was spoken, the only ending this
# side can time; the refusals cover a command the robot could not have acted on.
LINES = {key: Bag(pool) for key, pool in {
    "done": ("It is done.", "That is complete.", "The run is finished.",
             "Complete."),
    "unheard": ("I did not catch that.", "Say again?", "That did not come through.",
                "I missed that."),
    "disabled": ("The robot is disabled, such command is not possible.",
                 "The robot is disabled. I cannot do that.",
                 "Not while the robot is disabled."),
    "offline": ("There is no link to the robot, such command is not possible.",
                "I have no link to the robot.",
                "The robot is not answering."),
    "how": ("All systems nominal.", "Running well, thank you.",
            "No faults to report.", "In good order."),
    "here": ("I am listening.", "Still here.", "Listening."),
    "hello": ("Hello.", "Good to hear from you.", "At your service.",
              "Standing by."),
    "thanks": ("My pleasure.", "Any time.", "Of course."),
    "who": ("I am this robot's voice, and its ears.",
            "The voice of this machine, at your service."),
    "help": ("Do whatever."),
}.items()}

# Helix must not answer itself. The microphone stays live while a reply plays,
# and the bare verbs are the ones that dispatch with no wake word ahead of them,
# so no line may contain those words or the wake word.
assert not ({WAKE} | BARE).intersection(
    word.strip(".,?").lower()
    for bag in (*ACKS.values(), SENT, *LINES.values())
    for line in bag.lines
    for word in line.split()
)

# Everything the recognizer is allowed to return. "[unk]" lets it reject speech
# that is not a command rather than forcing a match onto the nearest phrase.
GRAMMAR = json.dumps(
    sorted({word for phrase in PHRASES for word in phrase.split()} | {WAKE} | FILLER)
    + sorted({*UNITS, *TEENS, *TENS})
    + ["seconds", "second", "[unk]"]
)


class Speaker:
    """Helix's voice.

    Synthesis runs on its own thread, so a reply neither holds up the next
    command nor stops the microphone from hearing one. The engine is built in
    that thread because pyttsx3's Windows driver is bound to the thread that
    created it.

    Replies are spoken in the order they are asked for. An urgent one drops
    whatever is still waiting, so a refusal or an all-stop is not read out
    behind a backlog of acknowledgements.
    """

    def __init__(self, rate=None, voice=None, mute=False):
        self.lines = queue.Queue()
        self.speaking = threading.Event()
        self.rate = rate
        self.voice = voice
        self.enabled = bool(pyttsx3) and not mute

        if self.enabled:
            threading.Thread(target=self.run, daemon=True).start()
        elif not mute:
            print("  [no pyttsx3; Helix stays silent]")

    def say(self, line, urgent=False):
        """Queue a reply. Returns at once; the speaking happens elsewhere."""
        if urgent:
            while not self.lines.empty():
                try:
                    self.lines.get_nowait()
                except queue.Empty:
                    break
        print(f"  Helix: {line}")
        if self.enabled:
            self.lines.put(line)

    def busy(self):
        """True while a reply is being spoken."""
        return self.speaking.is_set()

    def run(self):
        """Speak queued replies until the process ends."""
        try:
            engine = pyttsx3.init()
        except Exception as error:  # noqa: BLE001 - any driver fault is fatal here
            print(f"  [speech unavailable: {error}]", file=sys.stderr)
            self.enabled = False
            return

        if self.rate:
            engine.setProperty("rate", self.rate)
        if self.voice:
            for installed in engine.getProperty("voices"):
                if self.voice.lower() in installed.name.lower():
                    engine.setProperty("voice", installed.id)
                    break

        while True:
            line = self.lines.get()
            self.speaking.set()
            try:
                engine.say(line)
                engine.runAndWait()
            except Exception as error:  # noqa: BLE001 - a bad line is not worth dying for
                print(f"  [speech failed: {error}]", file=sys.stderr)
            finally:
                self.speaking.clear()


def reply(verb, seconds):
    """Compose what Helix says back when a command goes out."""
    line = ACKS[verb].draw() if verb in ACKS else SENT.draw()
    if seconds and verb in TIMED:
        return f"{line} for {seconds} seconds."
    return f"{line}."


def answer(verb, instance, state):
    """Answer a phrase that dispatches nothing.

    A question about the robot is reported rather than drawn from a pool: it is
    the one thing said here that has to be true at the moment it is said.
    """
    if verb != "status":
        return LINES[verb].draw()
    if not instance.isConnected():
        return "I have no link to the robot."
    if not running(state):
        return "The link is good. The robot is disabled and waiting."
    return "The link is good and the robot is enabled."


def running(state):
    """True unless the robot is reporting itself disabled.

    An unpublished control word means the robot is not saying either way, which
    is no reason to refuse it; the command is published and the robot decides.
    """
    word = state.get()
    return not word.isValid() or bool(int(word.value()) & ENABLED)


def roborio(team):
    """Static address of a team's roboRIO, 10.TE.AM.2."""
    return f"10.{team // 100}.{team % 100}.2"


def locate(team, timeout=0.3):
    """Pick the NT server to talk to: a simulator on this machine, or the robot.

    Nothing is exchanged with it; this only asks whether the port is open, which
    is what separates a simulator running here from a real robot on the field.
    """
    try:
        socket.create_connection((SIM, PORT), timeout).close()
        return SIM
    except OSError:
        return roborio(team)


def number(words):
    """Read a spoken count in [1, 60], or None. The last whole number wins."""
    value = None
    index = 0
    while index < len(words):
        word = words[index]
        if word in TENS:
            value = TENS[word]

            # A unit directly after a ten adds to it, as in "forty five".
            if index + 1 < len(words) and words[index + 1] in UNITS:
                value += UNITS[words[index + 1]]
                index += 1
        elif word in TEENS:
            value = TEENS[word]
        elif word in UNITS:
            value = UNITS[word]
        index += 1

    return min(value, 60) if value else None


def parse(text):
    """Read a transcript into a (verb, seconds) pair, or None if unrecognized."""
    words = [word for word in text.split() if word not in FILLER]

    # Consume the wake word. Only the bare verbs may go without it.
    woken = bool(words) and words[0] == WAKE
    if woken:
        words = words[1:]

    # Match the longest phrase the remainder starts with.
    for phrase in sorted(PHRASES, key=lambda p: -len(p.split())):
        spoken = phrase.split()
        if words[: len(spoken)] != spoken:
            continue

        verb = PHRASES[phrase]
        if not woken and verb not in BARE:
            return None
        return verb, number(words[len(spoken):])

    return None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server", default=None,
                        help="NT server address, pinning the search to one "
                             "place. Found on its own when this is not given.")
    parser.add_argument("--team", type=int, default=2990,
                        help="team number, which fixes the roboRIO address")
    parser.add_argument("--model", default="model", help="Vosk model directory")
    parser.add_argument("--device", type=int, default=None, help="input device index")
    parser.add_argument("--mute", action="store_true", help="dispatch without speaking")
    parser.add_argument("--voice", default=None,
                        help="installed speech voice to answer in, matched by name")
    parser.add_argument("--speech-rate", type=int, default=None,
                        help="speech rate in words per minute")
    parser.add_argument("--half-duplex", action="store_true",
                        help="ignore the microphone while Helix is speaking, for "
                             "a room where it hears itself")
    args = parser.parse_args()

    # Connect as an NT4 client and claim the utterance entry. A simulator on
    # this machine is preferred to the roboRIO, so the same command starts
    # against either; --server pins it to one address instead.
    instance = NetworkTableInstance.getDefault()
    target = args.server or locate(args.team)
    instance.setServer(target)
    instance.startClient4("voice")
    utterance = instance.getStringTopic(ENTRY).publish()
    state = instance.getTopic(CONTROL).genericSubscribe()

    speaker = Speaker(rate=args.speech_rate, voice=args.voice, mute=args.mute)

    # Load the recognizer against the closed grammar.
    SetLogLevel(-1)
    recognizer = KaldiRecognizer(Model(args.model), RATE, GRAMMAR)

    blocks = queue.Queue()
    nonce = 0
    timer = None

    def capture(data, frames, time, status):
        if status:
            print(status, file=sys.stderr)
        blocks.put(bytes(data))

    connected = None
    looked = time.monotonic()

    def finished():
        """Report the end of a timed run.

        A run the robot no longer has -- disabled or lost since it started --
        never reached its end, so nothing is said about it.
        """
        if instance.isConnected() and running(state):
            speaker.say(LINES["done"].draw())

    print(f"listening for '{WAKE} ...'; publishing {ENTRY} to {target}")
    with sd.RawInputStream(samplerate=RATE, blocksize=8000, dtype="int16",
                           channels=1, device=args.device, callback=capture):
        while True:
            block = blocks.get()

            # Drop what the microphone picked up of Helix's own reply, for a
            # room where the two are close enough to matter. The recognizer is
            # reset so the speech either side of the gap is not run together.
            if args.half_duplex and speaker.busy():
                recognizer.Reset()
                continue

            # Report the link, so an utterance that reaches nothing is visible.
            if instance.isConnected() != connected:
                connected = instance.isConnected()
                print(f"  [{'connected to' if connected else 'NOT connected to'} {target}]")

            # Look elsewhere while there is nothing to talk to. A simulator
            # started after this, or closed, moves the target either way.
            if (not connected and not args.server
                    and time.monotonic() - looked > RELOOK):
                looked = time.monotonic()
                found = locate(args.team)
                if found != target:
                    target = found
                    instance.setServer(target)
                    print(f"  [looking for {target} instead]")

            if not recognizer.AcceptWaveform(block):
                continue

            text = json.loads(recognizer.Result()).get("text", "")
            command = parse(text)
            if command is None:
                if text:
                    print(f"  ignored: {text}")

                # Speech opening with the wake word was meant for Helix, so an
                # answer is owed even though nothing in it parsed.
                if text.split()[:1] == [WAKE]:
                    speaker.say(LINES["unheard"].draw())
                continue

            verb, seconds = command

            # Talk, which asks nothing of the robot and so is answered whatever
            # state it is in, or whether there is one listening at all.
            if verb in SPOKEN_ONLY:
                print(f"> {text}")
                speaker.say(answer(verb, instance, state))
                continue

            # Refuse what the robot cannot act on. Both of these would leave the
            # command unrun with nothing said about it.
            refusal = None
            if not instance.isConnected():
                refusal = LINES["offline"].draw()
            elif verb not in DISABLED_OK and not running(state):
                refusal = LINES["disabled"].draw()
            if refusal:
                print(f"> {text}  ->  refused")
                speaker.say(refusal, urgent=True)
                continue

            # A command supersedes the one before it, so whatever Helix was
            # going to report the end of is no longer running.
            if timer:
                timer.cancel()
                timer = None

            # Publish the command, nonced so a repeat still dispatches. Helix
            # answers as it goes out rather than after, and an all-stop is
            # spoken ahead of anything still queued.
            nonce += 1
            message = f"{nonce} {verb}" + (f" {seconds}" if seconds else "")
            speaker.say(reply(verb, seconds), urgent=(verb == "stop"))
            utterance.set(message)
            instance.flush()
            print(f"> {text}  ->  {message}")

            # Close out a run whose length was spoken. It is the only ending
            # this side can time; the rest pass without a word.
            if seconds and verb in TIMED:
                timer = threading.Timer(seconds, finished)
                timer.daemon = True
                timer.start()


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nstopped")

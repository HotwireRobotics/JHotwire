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

    pip install pyntcore sounddevice vosk
    python Voice.py [--server 127.0.0.1] [--model model]

The Vosk model is a directory, downloaded separately from
https://alphacephei.com/vosk/models -- vosk-model-small-en-us is enough for a
grammar this size.
"""

import argparse
import json
import queue
import sys

import sounddevice as sd
from ntcore import NetworkTableInstance
from vosk import KaldiRecognizer, Model, SetLogLevel

ENTRY = "/SmartDashboard/Voice/Utterance"
RATE = 16000

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

# Everything the recognizer is allowed to return. "[unk]" lets it reject speech
# that is not a command rather than forcing a match onto the nearest phrase.
GRAMMAR = json.dumps(
    sorted({word for phrase in VERBS for word in phrase.split()} | {WAKE})
    + sorted({*UNITS, *TEENS, *TENS})
    + ["seconds", "second", "[unk]"]
)


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
    words = text.split()

    # Consume the wake word. Only the bare verbs may go without it.
    woken = bool(words) and words[0] == WAKE
    if woken:
        words = words[1:]

    # Match the longest phrase the remainder starts with.
    for phrase in sorted(VERBS, key=lambda p: -len(p.split())):
        spoken = phrase.split()
        if words[: len(spoken)] != spoken:
            continue

        verb = VERBS[phrase]
        if not woken and verb not in BARE:
            return None
        return verb, number(words[len(spoken):])

    return None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server", default="127.0.0.1",
                        help="roboRIO address; 127.0.0.1 for simulation")
    parser.add_argument("--model", default="model", help="Vosk model directory")
    parser.add_argument("--device", type=int, default=None, help="input device index")
    args = parser.parse_args()

    # Connect as an NT4 client and claim the utterance entry.
    instance = NetworkTableInstance.getDefault()
    instance.setServer(args.server)
    instance.startClient4("voice")
    utterance = instance.getStringTopic(ENTRY).publish()

    # Load the recognizer against the closed grammar.
    SetLogLevel(-1)
    recognizer = KaldiRecognizer(Model(args.model), RATE, GRAMMAR)

    blocks = queue.Queue()
    nonce = 0

    def capture(data, frames, time, status):
        if status:
            print(status, file=sys.stderr)
        blocks.put(bytes(data))

    print(f"listening for '{WAKE} ...'; publishing to {args.server}{ENTRY}")
    with sd.RawInputStream(samplerate=RATE, blocksize=8000, dtype="int16",
                           channels=1, device=args.device, callback=capture):
        while True:
            if not recognizer.AcceptWaveform(blocks.get()):
                continue

            text = json.loads(recognizer.Result()).get("text", "")
            command = parse(text)
            if command is None:
                if text:
                    print(f"  ignored: {text}")
                continue

            # Publish the command, nonced so a repeat still dispatches.
            verb, seconds = command
            nonce += 1
            message = f"{nonce} {verb}" + (f" {seconds}" if seconds else "")
            utterance.set(message)
            instance.flush()
            print(f"> {text}  ->  {message}")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nstopped")

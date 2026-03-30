from enum import Enum
import time, pygame as pg
import pygame_gui as pgui
import networktables as nt, io
import os, sys, math, requests
from math import prod
from networktables import NetworkTables as NT, NetworkTable

# Initialize handlers.
pg.init()
pg.mixer.init()

# Path pointers.
base: str = os.path.dirname(os.path.abspath(__file__))
deploy: str = os.path.join(base, "src", "main", "deploy")

# Dashboard state.
class State(Enum):
	REAL: int = 0
	SIM:  int = 1
	TEST: int = 2

########################################################################################

# Server target.
SERVER: str = "1.29.90.10"

# Instanciate state.
state: State = State.TEST

# Read specific assets.
sound: pg.Sound = pg.mixer.music.load(os.path.join(base, "assets", "rebuilt.mp3"))
field: pg.Surface = pg.image.load(os.path.join(base, "assets", "field.png"))

########################################################################################

# Set speed target.
FPS: int = 60

# Window state.
RUNNING: bool = True


# Initialize a real server; otherwise, use localhost.
if (state == State.REAL): NT.initialize(server=SERVER)
else:                NT.initialize(server="127.0.0.1")

# Get network table pointers.
table: NetworkTable =       NT.getTable("AdvantageKit")
driverStation =      table.getSubTable("DriverStation")
dashboard: NetworkTable = NT.getTable("SmartDashboard")
outputs =              table.getSubTable("RealOutputs")
camera: NetworkTable =   NT.getTable("CameraPublisher")
limelight =         camera.getSubTable("limelight-one")

# Set window caption.
pg.display.set_caption(f"Firelight - {SERVER}")

# Set window icon.
# pg.display.set_icon(pg.image.load(os.path.join(base, "logo.png")))

# Scan for screen size.
screen = pg.display.set_mode((0, 0))
screen_width, screen_height = screen.get_size()

# Fill screen.
size: tuple = (screen_width, int(screen_height * 0.7))

# Initialize GUI manager.
manager: pgui.UIManager = pgui.UIManager(size, os.path.join(base, "theme", "theme.json"))

# Track time and create render.
clock: pg.Clock = pg.Clock()
screen: pg.Surface = pg.display.set_mode(size, pg.SRCALPHA)

########################################################################################

# Topbar.
topbar_height = int(size[1] * 0.05)
topbar = pgui.elements.UIPanel(
    relative_rect=pg.Rect(0, 0, size[0], topbar_height),
    starting_height=1,
    manager=manager
)

# Field panel and image.
padding_top = 15  # distance below topbar
field_rect = pg.Rect(
    (size[0] - 800) // 2,
    topbar_height + padding_top,
    800, 400
)

# Add bounding box
field_panel = pgui.elements.UIPanel(
    relative_rect=field_rect,
    starting_height=0,
    manager=manager,
    object_id="#field_panel"
)

# Scale surface and add image
field_image = pgui.elements.UIImage(
    relative_rect=pg.Rect(0, 0, field_rect.width, field_rect.height),
    image_surface=pg.transform.smoothscale(field, (field_rect.width, field_rect.height)),
    manager=manager,
    container=field_panel
)

# Padding and layout
text_width = field_rect.x - 20

# Text panel.
text_rect = pg.Rect(
    10,
    topbar_height + 15,
    text_width, 200
)

text_panel = pgui.elements.UIPanel(
    relative_rect=text_rect,
    starting_height=0,
    manager=manager,
    object_id="#field_panel"
)

# Text label inside the panel.
match_time_label = pgui.elements.UILabel(
    relative_rect=pg.Rect(0, 0, text_rect.width, text_rect.height),
    text="0",  # initial value.
    manager=manager,
    container=text_panel,
    object_id="#field_text"
)

########################################################################################

# Startup sound.
# pg.mixer.music.play()

# Dashboard main loop.
def main() -> None:
	while RUNNING:
		# Change in timestamp.
		dtime: float = clock.tick(FPS)

		# Robot enabled state.
		enabled: bool = table.getBoolean("Enabled", False)
		
		# TODO: Add boolean indicator for enabled state.
		
    # TODO: Add autonomous selector and visualize path on field.
	
    # TODO: Add mechanism visualization for testing and systems check.
	  #  - User inputs for visual checks. (Possibly via a notification system)
	

		# Fill screen.
		screen.fill((20, 15, 15))

		events: list[pg.Event] = pg.event.get()
		for event in events:
			if event.type == pg.QUIT:
				pg.quit()
				sys.exit()

		# Update GUI manager.
		manager.update(dtime)

		# Collect network tables data.
		if ((state == State.REAL) or (state == State.SIM)):
			timestamp: float = driverStation.getNumber("MatchTime", -1)
			robotPose: list[float, float, float] = dashboard.getNumberArray("robot-pose", [1, 1, 0.5])
		if (state == State.TEST):
			timestamp: float = -1
			robotPose: list[float, float, float] = [0, 0, 0.5]
		
		# Update with data.
		match_time_label.set_text(f"{timestamp:.1f}")
		
		# Draw GUI.
		manager.draw_ui(screen)

		# Update screen.
		pg.display.flip()

if __name__ == '__main__':
	main()
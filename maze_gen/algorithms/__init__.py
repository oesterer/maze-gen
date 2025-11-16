from .base import EMPTY, HALLWAY, ROOM, MazeGenerator, Room
from .rooms_and_corridors import RoomsAndCorridorsGenerator

ALGORITHMS = {
    "rooms_and_corridors": RoomsAndCorridorsGenerator,
}

__all__ = [
    "ALGORITHMS",
    "MazeGenerator",
    "Room",
    "EMPTY",
    "ROOM",
    "HALLWAY",
    "RoomsAndCorridorsGenerator",
]

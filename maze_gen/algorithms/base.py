from __future__ import annotations

from dataclasses import dataclass
from typing import List, Protocol, Tuple

from ..config import MazeConfig

# Tile definitions
EMPTY = 0
ROOM = 1
HALLWAY = 2


@dataclass
class Room:
    x: int
    y: int
    width: int
    height: int

    @property
    def center(self) -> Tuple[int, int]:
        return (self.x + self.width // 2, self.y + self.height // 2)


class MazeGenerator(Protocol):
    def generate(self, config: MazeConfig) -> Tuple[List[List[int]], List[Room]]:
        ...

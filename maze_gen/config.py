from __future__ import annotations

from dataclasses import dataclass
from typing import Optional


@dataclass
class MazeConfig:
    width: int = 1000
    height: int = 1000
    min_room_width: int = 2
    min_room_height: int = 2
    max_room_width: int = 200
    max_room_height: int = 200
    coverage: float = 0.5  # fraction of area (0-1)
    hallway_width: int = 1
    algorithm: str = "rooms_and_corridors"
    output_path: str = "maze.png"
    seed: Optional[int] = None

    @property
    def area(self) -> int:
        return self.width * self.height

    @classmethod
    def from_args(cls, args: "argparse.Namespace") -> "MazeConfig":
        coverage = cls._parse_coverage(args.coverage)
        return cls(
            width=args.width,
            height=args.height,
            min_room_width=args.min_room_width,
            min_room_height=args.min_room_height,
            max_room_width=args.max_room_width,
            max_room_height=args.max_room_height,
            coverage=coverage,
            hallway_width=args.hallway_width,
            algorithm=args.algorithm,
            output_path=args.output,
            seed=args.seed,
        )

    @staticmethod
    def _parse_coverage(raw: float) -> float:
        # Accept values in range 0-1 or percentages 0-100.
        if raw > 1:
            return max(0.0, min(1.0, raw / 100.0))
        return max(0.0, min(1.0, raw))

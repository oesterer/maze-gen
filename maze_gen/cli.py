from __future__ import annotations

import argparse
from typing import Type

from . import __version__
from .algorithms import ALGORITHMS, MazeGenerator
from .config import MazeConfig
from .renderer import render_ppm
from .stats import count_coverage


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Generate a room-and-corridor maze and render to a PPM image")
    parser.add_argument("--width", type=int, default=1000, help="Area width (default: 1000)")
    parser.add_argument("--height", type=int, default=1000, help="Area height (default: 1000)")
    parser.add_argument("--min-room-width", type=int, default=2, dest="min_room_width", help="Minimum room width (default: 2)")
    parser.add_argument("--min-room-height", type=int, default=2, dest="min_room_height", help="Minimum room height (default: 2)")
    parser.add_argument("--max-room-width", type=int, default=200, dest="max_room_width", help="Maximum room width (default: 200)")
    parser.add_argument("--max-room-height", type=int, default=200, dest="max_room_height", help="Maximum room height (default: 200)")
    parser.add_argument(
        "--coverage",
        type=float,
        default=50.0,
        help="Minimum coverage as percentage (e.g. 50) or fraction (e.g. 0.5). Default 50%%",
    )
    parser.add_argument("--hallway-width", type=int, default=1, dest="hallway_width", help="Hallway width in tiles (default: 1)")
    parser.add_argument(
        "--algorithm",
        choices=sorted(ALGORITHMS.keys()),
        default="rooms_and_corridors",
        help="Maze generation algorithm",
    )
    parser.add_argument("--output", default="maze.ppm", help="Output image path (PPM format)")
    parser.add_argument("--seed", type=int, default=None, help="Optional random seed for reproducible mazes")
    parser.add_argument("--version", action="version", version=f"%(prog)s {__version__}")
    return parser


def load_generator(name: str) -> MazeGenerator:
    cls: Type[MazeGenerator] | None = ALGORITHMS.get(name)
    if cls is None:
        raise ValueError(f"Unknown algorithm '{name}'")
    return cls()


def main(argv: list[str] | None = None) -> None:
    parser = build_parser()
    args = parser.parse_args(argv)

    config = MazeConfig.from_args(args)
    generator = load_generator(config.algorithm)

    grid, rooms = generator.generate(config)
    render_ppm(grid, config.output_path)

    filled_cells, coverage = count_coverage(grid)
    print(
        f"Generated {len(rooms)} rooms with algorithm '{config.algorithm}'. "
        f"Coverage: {coverage:.1%} ({filled_cells}/{config.area} tiles). Output: {config.output_path}"
    )


if __name__ == "__main__":
    main()

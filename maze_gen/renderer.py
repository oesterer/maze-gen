from __future__ import annotations

from typing import List

from .algorithms import EMPTY, HALLWAY, ROOM

# Colors: light gray empty, blue rooms, dark hallways.
COLORS = {
    EMPTY: (245, 245, 245),
    ROOM: (50, 130, 255),
    HALLWAY: (30, 30, 30),
}


def render_ppm(grid: List[List[int]], path: str) -> None:
    if not grid:
        raise ValueError("Grid is empty; nothing to render")

    height = len(grid)
    width = len(grid[0])

    header = f"P6\n{width} {height}\n255\n".encode("ascii")

    with open(path, "wb") as f:
        f.write(header)
        for row in grid:
            buf = bytearray()
            for cell in row:
                color = COLORS.get(cell, COLORS[EMPTY])
                buf.extend(color)
            f.write(buf)

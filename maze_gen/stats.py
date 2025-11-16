from __future__ import annotations

from typing import List, Tuple

from .algorithms import EMPTY


def count_coverage(grid: List[List[int]]) -> Tuple[int, float]:
    if not grid:
        return 0, 0.0
    filled = 0
    height = len(grid)
    width = len(grid[0])
    for row in grid:
        for cell in row:
            if cell != EMPTY:
                filled += 1
    total = width * height
    return filled, filled / total if total else 0.0

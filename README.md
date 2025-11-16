# Maze Generator

A small Python tool that builds a tiled maze made of rectangular rooms connected by hallways, then renders the layout to a PPM image. The code is structured so different generation algorithms can be plugged in via `maze_gen/algorithms`.

## How it works
- Grid-based: the area is a 2D array of tiles; each tile is empty, room, or hallway.
- Configurable parameters: area size, min/max room size, target coverage (rooms + hallways), hallway width, algorithm, and random seed.
- Pluggable algorithms: the CLI picks an algorithm by name from `maze_gen.algorithms.ALGORITHMS`; new ones can be added without changing the CLI.
- Renderer: outputs a binary PPM (`P6`) image using light gray for empty tiles, blue for rooms, and dark gray for hallways.

## Usage
Run via `python3 -m maze_gen.cli`:

```bash
python3 -m maze_gen.cli \
  --width 1000 --height 1000 \
  --min-room-width 2 --min-room-height 2 \
  --max-room-width 200 --max-room-height 200 \
  --coverage 50 \
  --hallway-width 1 \
  --algorithm rooms_and_corridors \
  --output maze.ppm \
  [--seed 1234]
```

Notes:
- `--coverage` accepts `0-1` (fraction) or `0-100` (percent). Default is 50 (50%).
- `--algorithm` selects the generator implementation; defaults to `rooms_and_corridors`.
- The output is a PPM image you can view with most image tools (`P6` binary format).
- Everything is pure Python; no external dependencies are required.

## Default algorithm: rooms_and_corridors
The bundled algorithm follows a simple rooms-and-corridors approach to ensure all rooms connect:
- **Room placement:** randomly sample room sizes within min/max bounds and place them at random positions as long as they do not overlap existing rooms. Stop when the coverage target is reached or attempts are exhausted.
- **Graph connection:** treat each room’s center as a node; build a loose spanning tree using a greedy/Prim-like step that connects the nearest unconnected room to the current set.
- **Corridor carving:** for each edge in the spanning tree, carve an L-shaped hallway (randomly choosing horizontal-first or vertical-first) with the configured hallway width, marking tiles as hallways and increasing coverage.
- **Output:** return the tile grid and room list; the renderer then writes the PPM image and the CLI prints achieved coverage.

## Adding your own algorithm
Implement `MazeGenerator.generate(config)` returning `(grid, rooms)` in a new module under `maze_gen/algorithms`, then register the class in `ALGORITHMS` inside `maze_gen/algorithms/__init__.py` so it becomes selectable with `--algorithm`.

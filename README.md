# Maze Generator

A small Python tool that builds a tiled maze made of rectangular rooms connected by hallways, then renders the layout to a PNG image. The code is structured so different generation algorithms can be plugged in via `maze_gen/algorithms`.

## How it works
- Grid-based: the area is a 2D array of tiles; each tile is empty, room, or hallway.
- Configurable parameters: area size, min/max room size, target coverage (rooms + hallways), hallway width, algorithm, and random seed.
- Pluggable algorithms: the CLI picks an algorithm by name from `maze_gen.algorithms.ALGORITHMS`; new ones can be added without changing the CLI.
- Renderer: outputs a PNG image using light gray for empty tiles, medium gray for rooms, and very dark gray/black for hallways for clear contrast.

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
  --output maze.png \
  [--seed 1234]
```

Notes:
- `--coverage` accepts `0-1` (fraction) or `0-100` (percent). Default is 50 (50%).
- `--algorithm` selects the generator implementation; defaults to `rooms_and_corridors`.
- The output is a PNG image that most image tools can open.
- Everything is pure Python; no external dependencies are required.

### Java version (Maven)

A Java implementation lives in `java/src/main/java`. Build, test, and run with Maven:

```bash
mvn -f java/pom.xml clean package
mvn -f java/pom.xml exec:java -Dexec.args="\
  --width 1000 --height 1000 \
  --min-room-width 2 --min-room-height 2 \
  --max-room-width 200 --max-room-height 200 \
  --coverage 50 \
  --hallway-width 1 \
  --algorithm rooms_and_corridors \
  --output maze.png \
  [--seed 1234]" 
```

Flags mirror the Python CLI. The output is the same PNG color scheme (light gray empty, medium gray rooms, very dark hallways).

## Default algorithm: rooms_and_corridors
The bundled algorithm follows a simple rooms-and-corridors approach to ensure all rooms connect:
- **Room placement:** randomly sample room sizes within min/max bounds and place them at random positions as long as they do not overlap existing rooms. Stop when the coverage target is reached or attempts are exhausted.
- **Graph connection:** treat each room’s center as a node; build a loose spanning tree using a greedy/Prim-like step that connects the nearest unconnected room to the current set.
- **Corridor carving:** for each edge in the spanning tree, carve an L-shaped hallway (randomly choosing horizontal-first or vertical-first) with the configured hallway width, stopping at room boundaries so rooms stay intact.
- **Dead-end pruning:** after carving, remove hallway dead ends that are not adjacent to rooms, leaving a cleaner network.
- **Output:** return the tile grid and room list; the renderer then writes the PNG image and the CLI prints achieved coverage.

## Adding your own algorithm
Implement `MazeGenerator.generate(config)` returning `(grid, rooms)` in a new module under `maze_gen/algorithms`, then register the class in `ALGORITHMS` inside `maze_gen/algorithms/__init__.py` so it becomes selectable with `--algorithm`.

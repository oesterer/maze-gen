package maze;

import java.util.List;

public final class MazeResult {
    public final Tile[][] grid;
    public final List<Room> rooms;

    public MazeResult(Tile[][] grid, List<Room> rooms) {
        this.grid = grid;
        this.rooms = rooms;
    }
}

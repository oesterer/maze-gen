package maze;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public final class MazeRenderer {
    private static final Color EMPTY = new Color(245, 245, 245);
    private static final Color ROOM = new Color(180, 180, 180);
    private static final Color HALLWAY = new Color(10, 10, 10);

    private MazeRenderer() {}

    public static void renderPng(Tile[][] grid, String path) throws IOException {
        if (grid.length == 0) throw new IllegalArgumentException("Grid is empty");
        int height = grid.length;
        int width = grid[0].length;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, colorFor(grid[y][x]).getRGB());
            }
        }
        ImageIO.write(img, "PNG", new File(path));
    }

    private static Color colorFor(Tile tile) {
        return switch (tile) {
            case ROOM -> ROOM;
            case HALLWAY -> HALLWAY;
            case EMPTY -> EMPTY;
        };
    }
}

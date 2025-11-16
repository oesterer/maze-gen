package maze;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class Main {
    private static final Map<String, MazeGenerator> GENERATORS = new HashMap<>();
    static {
        GENERATORS.put("rooms_and_corridors", new RoomsAndCorridorsGenerator());
    }

    public static void main(String[] args) {
        MazeConfig config;
        try {
            config = MazeConfig.fromArgs(args);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            MazeConfig.printHelp();
            System.exit(1);
            return;
        }

        MazeGenerator generator = GENERATORS.get(config.algorithm);
        if (generator == null) {
            System.err.println("Unknown algorithm: " + config.algorithm);
            System.exit(1);
            return;
        }

        MazeResult result = generator.generate(config);
        try {
            MazeRenderer.renderPng(result.grid, config.outputPath);
        } catch (IOException e) {
            System.err.println("Failed to write image: " + e.getMessage());
            System.exit(1);
        }

        int filled = 0;
        for (Tile[] row : result.grid) {
            for (Tile t : row) {
                if (t != Tile.EMPTY) filled++;
            }
        }
        double coverage = (double) filled / config.area();
        System.out.printf(
                "Generated %d rooms with '%s'. Coverage: %.1f%% (%d/%d tiles). Output: %s%n",
                result.rooms.size(),
                config.algorithm,
                coverage * 100.0,
                filled,
                config.area(),
                config.outputPath);
    }
}

package dyatel.terracontrol.level.generation;

import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;

public class SymmetricGenerator extends Generator {

    private int y;

    public SymmetricGenerator(GeneratableLevel level) {
        super(level);
    }

    protected void gen() {
        int width = level.getWidth();
        int height = level.getHeight();
        int segmentWidth = width / 2;
        int segmentHeight = height / 2;

        for (int x = 0; x < segmentWidth; x++) {
            int colorID = random.nextInt(level.getColors().length);

            new Cell(x, y, new CellMaster(colorID, level)); // Top left
            new Cell(width - 1 - x, y, new CellMaster(colorID, level)); // Top right
            new Cell(x, height - 1 - y, new CellMaster(colorID, level)); // Bottom left
            new Cell(width - 1 - x, height - 1 - y, new CellMaster(colorID, level)); // Bottom right
        }

        if (y < segmentHeight - 1) {
            y++;
        } else {
            if (width % 2 == 1) {
                for (int y = 0; y < height; y++) {
                    new Cell(segmentWidth, y, new CellMaster(level));
                }
            }

            if (height % 2 == 1) {
                for (int x = 0; x < width; x++) {
                    new Cell(x, segmentHeight, new CellMaster(level));
                }
            }
        }
    }

    public String getName() {
        return "Symmetric";
    }

}

package dyatel.terracontrol.level.generation;

import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;

public class LinearGenerator extends Generator {

    public LinearGenerator(GeneratableLevel level) {
        super(level);
    }

    protected void gen() {
        int width = level.getWidth();
        int height = level.getHeight();

        for (int y = 0; y < height; y++) {
            int colorID = random.nextInt(level.getColors().length);
            for (int x = 0; x < width; x++) {
                int yy = y + random.nextInt(3) - 1;
                if (level.canSetCell(x, yy)) {
                    new Cell(x, yy, new CellMaster(colorID, level));
                }
            }
        }
    }

    public String getName() {
        return "Linear";
    }

}

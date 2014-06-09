package dyatel.terracontrol.level.generation;

import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;

public class LinearGenerator extends Generator {

    private int y = 0;

    public LinearGenerator(GeneratableLevel level) {
        super(level);
    }

    protected void gen() {
        int width = level.getWidth();
        int height = level.getHeight();

        int colorID = random.nextInt(level.getColors().length);
        for (int x = 0; x < width; x++) {
            int yy = y + random.nextInt(3) - 1;
            if (level.canSetCell(x, yy)) {
                new Cell(x, yy, new CellMaster(colorID, level));
            }
        }

        if (y < height - 1)
            y++;
        else {
            for (int i = 0; i < width * height; i++) {
                if (level.canSetCell(i % width, i / width)) {
                    new Cell(i % width, i / width, new CellMaster(level));
                }
            }
        }
    }

    public String getName() {
        return "Linear";
    }

}

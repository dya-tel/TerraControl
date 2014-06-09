package dyatel.terracontrol.level.generation;

import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;

public class FillGenerator extends Generator {

    private int y = 0;

    public FillGenerator(GeneratableLevel level) {
        super(level);
    }

    protected void gen() {
        // Filling whole field with masters
        int width = level.getWidth();
        int height = level.getHeight();

        for (int x = 0; x < width; x++) {
            new Cell(x, y, new CellMaster(level));
        }

        if (y < height - 1) y++;
    }

    public String getName() {
        return "Fill";
    }

}

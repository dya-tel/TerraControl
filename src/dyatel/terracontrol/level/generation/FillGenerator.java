package dyatel.terracontrol.level.generation;

import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;

public class FillGenerator extends Generator {

    public FillGenerator(GeneratableLevel level) {
        super(level);
    }

    protected void gen() {
        // Filling whole field with masters
        int width = level.getWidth();
        int height = level.getHeight();
        for (int i = 0; i < width * height; i++) {
            new Cell(i % width, i / width, new CellMaster(level));
        }
    }

    public String getName() {
        return "Fill";
    }

}

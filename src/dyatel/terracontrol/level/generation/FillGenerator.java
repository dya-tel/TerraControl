package dyatel.terracontrol.level.generation;

import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;

public class FillGenerator extends Generator {

    public FillGenerator(GeneratableLevel level) {
        super(level);
    }

    public void generate(Cell[] cells) {
        if (genStart == -1) genStart = System.currentTimeMillis();

        int width = level.getWidth();
        for (int i = 0; i < cells.length; i++) {
            new Cell(i % width, i / width, new CellMaster(level));
        }

        onLevelGenerated();
    }

    public static String getName() {
        return "Fill";
    }

}

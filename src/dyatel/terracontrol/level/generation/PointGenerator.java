package dyatel.terracontrol.level.generation;

import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;
import dyatel.terracontrol.util.Debug;

public class PointGenerator extends Generator {

    public PointGenerator(GeneratableLevel level, int min, int max) {
        super(level);

        Debug debug = level.getDebug();

        // Adding masters
        int masters = random.nextInt(max - min + 1) + min;
        int width = level.getWidth();
        int height = level.getHeight();
        debug.println("Going to add " + masters + " masters");
        for (int i = 0; i < masters; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            if (level.getCell(x, y) == null) {
                new Cell(x, y, new CellMaster(level));
            }
        }
        debug.println("Added " + level.getMasters().size() + " masters");
    }

    public void generate(Cell[] cells) {
        if (genStart == -1) genStart = System.currentTimeMillis();

        for (CellMaster master : level.getMasters()) master.generate();
        if (isGenerated()) onLevelGenerated();
    }
}

package dyatel.terracontrol.level.generation;

import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;
import dyatel.terracontrol.util.Debug;

public class PointGenerator extends Generator {

    public PointGenerator(GeneratableLevel level) {
        super(level);

        Debug debug = level.getDebug();

        // Adding masters
        int width = level.getWidth();
        int height = level.getHeight();
        int minMasters = width * height * 2 / 5;
        int maxMasters = width * height * 4 / 5;
        int masters = random.nextInt(maxMasters - minMasters + 1) + minMasters;
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

    protected void gen(Cell[] cells) {
        for (CellMaster master : level.getMasters()) master.generate(); // Generating every master
    }

    public String getName() {
        return "Point";
    }

}

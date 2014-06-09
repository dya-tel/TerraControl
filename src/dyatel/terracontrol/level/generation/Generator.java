package dyatel.terracontrol.level.generation;

import dyatel.terracontrol.level.CellMaster;
import dyatel.terracontrol.util.Util;

import java.util.ArrayList;
import java.util.Random;

public abstract class Generator {

    protected Random random = Util.getRandom(); // Randomizer

    protected GeneratableLevel level; // Level that we are generating

    protected long genStart = -1; // System time at the moment of first generate() call

    public static final String[] types = {"Fill", "Point", "Symmetric", "Linear"}; // All available generators

    public Generator(GeneratableLevel level) {
        this.level = level;
        level.getDebug().println("Using \"" + getName() + "\" generator");
    }

    public final void generate() {
        if (genStart == -1) genStart = System.currentTimeMillis();

        // Checking if level is generated
        if (isGenerated()) {
            level.getDebug().println("Generated level in " + (System.currentTimeMillis() - genStart) + " ms");
            onLevelGenerated();
            return;
        }

        gen();
    }

    // Generation algorithm
    protected abstract void gen();

    protected void onLevelGenerated() {
        level.getWindow().statusBar[1] = "Calculating borders";
        level.getWindow().render();

        ArrayList<CellMaster> masters = level.getMasters();
        for (int i = 0; i < masters.size(); i++) {
            CellMaster master = masters.get(i);
            master.setID(i); // Setting ID to master
            level.needUpdate(master); // Updating master to calculate borders and find neighbors
        }

        level.onLevelGenerated(); // Saying level that we are done
    }

    public int getGeneratedPercent() {
        int generated = 0;
        for (CellMaster master : level.getMasters()) generated += master.getCells().size();
        return generated * 100 / (level.getWidth() * level.getHeight());
    }

    public boolean isGenerated() {
        int width = level.getWidth();
        for (int i = 0; i < width * level.getHeight(); i++) {
            if (level.getCell(i % width, i / width) == null) return false; // Not generated if there is null cell
        }
        return true;
    }

    public abstract String getName();

    public static Generator parseGenerator(String string, GeneratableLevel level) {
        // Finding generator
        if (string.equals("Fill")) return new FillGenerator(level);
        if (string.equals("Point")) return new PointGenerator(level);
        if (string.equals("Symmetric")) return new SymmetricGenerator(level);
        if (string.equals("Linear")) return new LinearGenerator(level);
        return null; // If found nothing
    }

}

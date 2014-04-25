package dyatel.terracontrol.level;

import dyatel.terracontrol.Screen;
import dyatel.terracontrol.input.Keyboard;
import dyatel.terracontrol.input.Mouse;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.util.Debug;
import dyatel.terracontrol.window.GameWindow;

import java.util.ArrayList;

public abstract class Level {

    protected GameWindow window; // Main window
    protected Debug debug; // Output

    protected int xOff, yOff; // Level offset
    protected int scrollRate = 5; // Pixels per update

    protected int cellSize;
    protected double zoom = 1;

    protected int width, height; // Level size in cells

    protected Keyboard keyboard; // Keyboard listener
    protected int keyDelay; // Timer that restricts pressing a key every update (60 times per second!)

    protected Mouse mouse; // Mouse listener
    protected int mouseX, mouseY; // Real mouse coordinates based on position in window
    protected int mouseLX, mouseLY; // Mouse coordinates used on level

    protected ArrayList<CellMaster> masters;
    protected ArrayList<Updatable> needUpdate;

    protected Cell[] cells;

    protected int timer = 0;
    protected int delay = 20;

    protected int[] colors;

    protected Level(int cellSize, GameWindow window) {
        this.cellSize = cellSize;

        this.window = window;
        debug = window.getDebug();

        // Receiving and initializing input
        keyboard = window.getKeyboard();
        mouse = window.getMouse();
        mouse.setLevel(this);

        masters = new ArrayList<CellMaster>();
        needUpdate = new ArrayList<Updatable>();
    }

    public abstract void init(DataArray data);

    public abstract void update();

    public abstract void render(Screen screen);

    public boolean canSetCell(int x, int y) {
        // If this coordinates belong to level and there is noting on them, returning true
        return x >= 0 && x < width && y >= 0 && y < height && getCell(x, y) == null;
    }

    public Cell getCell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        return cells == null ? null : cells[x + y * width];
    }

    public CellMaster getMaster(int x, int y) {
        // Safe method to get masters without checking cells
        if (getCell(x, y) == null) return null;
        return getCell(x, y).getMaster();
    }

    public void setCell(Cell cell) {
        cells[cell.getX() + cell.getY() * width] = cell;
    }

    public void add(CellMaster u) {
        masters.add(u);
    }

    public void needUpdate(Updatable u) {
        needUpdate.add(u);
    }

    public int getCellSize() {
        return (int) (cellSize * zoom);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ArrayList<CellMaster> getMasters() {
        return masters;
    }

    public int[] getColors() {
        return colors;
    }

    public int getColorID(int color) {
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == color) return i;
        }
        return -1;
    }

    public void changeZoom(int n) {
        //TODO: fix this already
        if (cellSize * (zoom + n * 0.1d) < 1) return; // Returning if zoomed too much

        zoom += n * 0.1d;

        // Scaling offset
        double scale = zoom / (zoom - n * 0.1d);
        xOff *= scale;
        yOff *= scale;
    }

    public Debug getDebug() {
        return debug;
    }

}

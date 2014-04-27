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

    protected boolean initialized = false;

    protected int xOff, yOff; // Level offset
    protected int scrollRate = 5; // Pixels per update

    protected int cellSize;
    protected double zoom = 1;

    protected int width, height; // Level size in cells

    protected Keyboard keyboard; // Keyboard listener
    protected int keyDelay; // Timer that restricts pressing a key every update (60 times per second!)
    protected boolean[] keys;

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

    public final void update() {
        // Updating input
        // Updating key delay, getting key state
        if (keyDelay > -1) keyDelay--;
        keys = keyboard.getKeys();

        // Updating mouse coordinates
        mouseX = mouse.getX();
        mouseY = mouse.getY();
        if (mouseX > -1 && mouseY > -1 && mouseY < window.getFieldHeight()) {
            mouseLX = (mouseX + xOff) / (getCellSize() + 1);
            mouseLY = (mouseY + yOff) / (getCellSize() + 1);
        } else {
            mouseLX = -1;
            mouseLY = -1;
        }

        // Updating offset if needed
        if (keys[10]) xOff -= scrollRate;
        if (keys[11]) yOff -= scrollRate;
        if (keys[12]) xOff += scrollRate;
        if (keys[13]) yOff += scrollRate;

        if (xOff < 0) xOff = 0;
        if (xOff + window.getWidth() > width * (getCellSize() + 1) - 1)
            xOff = Math.max(width * (getCellSize() + 1) - 1 - window.getWidth(), 0);
        if (yOff < 0) yOff = 0;
        if (yOff + window.getFieldHeight() > height * (getCellSize() + 1) - 1)
            yOff = Math.max(height * (getCellSize() + 1) - 1 - window.getFieldHeight(), 0);

        if (!initialized) return;

        // Update-on-demand
        while (needUpdate.size() > 0) {
            Updatable u = needUpdate.get(0);
            if (u != null && !u.isRemoved()) {
                u.update();
            } else {
                if (u instanceof CellMaster) masters.remove(u);
            }
            needUpdate.remove(0);
        }

        // Server/client update
        sideUpdate();
    }

    // One-side update, specific for client and server
    protected abstract void sideUpdate();

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

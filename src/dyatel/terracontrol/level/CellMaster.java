package dyatel.terracontrol.level;

import dyatel.terracontrol.Server;
import dyatel.terracontrol.util.Util;

import java.util.ArrayList;
import java.util.Random;

public class CellMaster implements Updatable {

    private int color;

    private int id;

    private ArrayList<Cell> newCells = new ArrayList<Cell>();
    private ArrayList<Cell> cells = new ArrayList<Cell>();
    private ArrayList<Cell> borderCells = new ArrayList<Cell>();

    private ArrayList<CellMaster> neighbors = new ArrayList<CellMaster>();

    private Owner owner = null;

    private Level level;

    private boolean removed = false;

    private static Random random = Util.getRandom();

    public CellMaster(Level level) {
        this(Server.colors[random.nextInt(Server.colors.length)], level);
    }

    public CellMaster(int color, Level level) {
        this.color = color;
        this.level = level;

        level.add(this);
    }

    public void tryToAdd(int x, int y) {
        if (level.canSetCell(x, y)) new Cell(x, y, this);
    }

    public void merge(CellMaster master) {
        // Updating cell lists and setting new master
        updateCellLists();
        for (Cell cell : cells) {
            cell.setMaster(master);
        }

        // Just giving cells, cause we`ll be deleted and other master already has right color and owner
        master.addCells(cells);

        remove();
    }

    private void updateCellLists() {
        // Return if nothing changed
        if (newCells.size() == 0) {
            return;
        }

        cells.addAll(newCells);
        newCells.clear();

        // Recalculating borders
        borderCells.clear();
        for (Cell cell : cells) {
            int smCells = 0;

            if (level.getCell(cell.getX(), cell.getY() - 1) != null && level.getCell(cell.getX(), cell.getY() - 1).getMaster() == this)
                smCells++;
            if (level.getCell(cell.getX() + 1, cell.getY()) != null && level.getCell(cell.getX() + 1, cell.getY()).getMaster() == this)
                smCells++;
            if (level.getCell(cell.getX(), cell.getY() + 1) != null && level.getCell(cell.getX(), cell.getY() + 1).getMaster() == this)
                smCells++;
            if (level.getCell(cell.getX() - 1, cell.getY()) != null && level.getCell(cell.getX() - 1, cell.getY()).getMaster() == this)
                smCells++;

            if (smCells < 4) {
                borderCells.add(cell);
            }
        }
    }

    private void updateNeighbors() {
        neighbors.clear();
        for (Cell cell : borderCells) {
            checkNeighbor(cell.getX(), cell.getY() - 1);
            checkNeighbor(cell.getX() + 1, cell.getY());
            checkNeighbor(cell.getX(), cell.getY() + 1);
            checkNeighbor(cell.getX() - 1, cell.getY());
        }
    }

    private void checkNeighbor(int x, int y) {
        if (level.getCell(x, y) != null) {
            CellMaster neighbor = level.getCell(x, y).getMaster();
            if (neighbor != this && neighbor.getColor() == color && (neighbor.getOwner() == owner || neighbor.getOwner() == null)) {
                neighbor.merge(this);
            } else {
                if (!neighbors.contains(neighbor) && neighbor != this) neighbors.add(neighbor);
            }
        }
    }

    public void generate() {
        for (Cell cell : borderCells) {
            int x = cell.getX();
            int y = cell.getY();
            switch (random.nextInt(4)) {
                case 0:
                    x -= 1;
                    break;
                case 1:
                    y -= 1;
                    break;
                case 2:
                    x += 1;
                    break;
                case 3:
                    y += 1;
                    break;
            }
            tryToAdd(x, y);
        }

        update();
    }

    public void update() {
        // Updating while updates change something
        do {
            updateCellLists();
            updateNeighbors();
        } while (newCells.size() > 0);
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public void setID(int id) {
        this.id = id;
    }

    public int getID() {
        return id;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public Owner getOwner() {
        return owner;
    }

    public void addCells(ArrayList<Cell> cells) {
        newCells.addAll(cells);
    }

    public void addCell(Cell cell) {
        newCells.add(cell);
    }

    public ArrayList<Cell> getCells() {
        return cells;
    }

    public boolean isNeighbor(CellMaster master) {
        return neighbors.contains(master);
    }

    public ArrayList<CellMaster> getNeighbors() {
        return neighbors;
    }

    public Level getLevel() {
        return level;
    }

    public void remove() {
        removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }

}

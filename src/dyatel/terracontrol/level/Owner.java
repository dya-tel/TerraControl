package dyatel.terracontrol.level;

public class Owner implements Updatable {

    private CellMaster master;

    private int id;

    private boolean removed = false;

    public Owner(int x, int y, int id, Level level) {
        this(level.getCell(x, y).getMaster(), id, level);
    }

    public Owner(CellMaster master, int id, Level level) {
        this.id = id;

        if (master.getOwner() == null) {
            level.getDebug().println("Creating owner at master " + master);
            this.master = master;
            master.setOwner(this);
            level.add(this);
        } else {
            level.getDebug().println("Failed creating owner " + this + " on master " + master + ": someone is already owning this master!");
        }
    }

    public void update() {
        master.update();
    }

    public CellMaster getMaster() {
        return master;
    }

    public void setColor(int color) {
        master.setColor(color);
        update();
    }

    public int getColor() {
        return master.getColor();
    }

    public int getID() {
        return id;
    }

    public void remove() {
        removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }

}

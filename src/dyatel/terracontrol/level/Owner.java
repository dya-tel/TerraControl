package dyatel.terracontrol.level;

public class Owner {

    private CellMaster master;

    private int id;

    private Level level;

    public Owner(int x, int y, int id, Level level) {
        this(level.getCell(x, y).getMaster(), id);
    }

    public Owner(CellMaster master, int id) {
        this.id = id;
        level = master.getLevel();

        if (master.getOwner() == null) {
            level.getDebug().println("Creating owner at master " + master);
            this.master = master;
            master.setOwner(this);
        } else {
            level.getDebug().println("Failed creating owner " + this + " on master " + master + ": someone is already owning this master!");
        }
    }

    public CellMaster getMaster() {
        return master;
    }

    public void setColor(int color) {
        master.setColor(color);
        level.needUpdate(master);
    }

    public int getColor() {
        return master.getColor();
    }

    public int getID() {
        return id;
    }

}

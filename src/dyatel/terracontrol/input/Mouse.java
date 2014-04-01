package dyatel.terracontrol.input;

import dyatel.terracontrol.level.Level;

import java.awt.event.*;

public class Mouse implements MouseListener, MouseMotionListener, MouseWheelListener {

    private Level level;

    private int x = -1, y = -1;

    private boolean clicked = false;

    public void mouseDragged(MouseEvent e) {
        x = e.getX();
        y = e.getY();
    }

    public void mouseMoved(MouseEvent e) {
        x = e.getX();
        y = e.getY();
    }

    public void mouseClicked(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {
        clicked = true;
    }

    public void mouseReleased(MouseEvent e) {
        clicked = false;
    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {
        x = -1;
        y = -1;
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        if (level != null) {
            level.changeZoom(e.getWheelRotation() * -1);
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isClicked() {
        return clicked;
    }

    public void setLevel(Level level) {
        this.level = level;
    }
}

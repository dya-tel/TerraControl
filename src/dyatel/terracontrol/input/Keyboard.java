package dyatel.terracontrol.input;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Keyboard extends KeyAdapter {

    // reset 0, colors 1 6, +delay 7, -delay 8, left 10, up 11, right 12, down 13
    private boolean[] keys;

    public Keyboard() {
        keys = new boolean[20];
    }

    private void changeState(int key, boolean state) {
        if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_6) {
            keys[1 + key - KeyEvent.VK_1] = state;
            return;
        }

        switch (key) {
            case KeyEvent.VK_SPACE:
                keys[0] = state;
                break;
            case KeyEvent.VK_ADD:
                keys[7] = state;
                break;
            case KeyEvent.VK_SUBTRACT:
                keys[8] = state;
                break;
            case KeyEvent.VK_LEFT:
                keys[10] = state;
                break;
            case KeyEvent.VK_UP:
                keys[11] = state;
                break;
            case KeyEvent.VK_RIGHT:
                keys[12] = state;
                break;
            case KeyEvent.VK_DOWN:
                keys[13] = state;
                break;
        }
    }

    public void keyPressed(KeyEvent e) {
        changeState(e.getKeyCode(), true);
    }

    public void keyReleased(KeyEvent e) {
        changeState(e.getKeyCode(), false);
    }

    public boolean[] getKeys() {
        return keys;
    }

}

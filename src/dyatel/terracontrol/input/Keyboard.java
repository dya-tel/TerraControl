package dyatel.terracontrol.input;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Keyboard extends KeyAdapter {

    public static final int KEY_LEFT = 0;
    public static final int KEY_UP = 1;
    public static final int KEY_RIGHT = 2;
    public static final int KEY_DOWN = 3;
    public static final int KEY_PLUS = 4;
    public static final int KEY_MINUS = 5;

    private boolean[] keys;

    public Keyboard() {
        keys = new boolean[6];
    }

    private void changeState(int key, boolean state) {
        switch (key) {
            case KeyEvent.VK_LEFT:
                keys[KEY_LEFT] = state;
                break;
            case KeyEvent.VK_UP:
                keys[KEY_UP] = state;
                break;
            case KeyEvent.VK_RIGHT:
                keys[KEY_RIGHT] = state;
                break;
            case KeyEvent.VK_DOWN:
                keys[KEY_DOWN] = state;
                break;
            case KeyEvent.VK_EQUALS:
                keys[KEY_PLUS] = state;
                break;
            case KeyEvent.VK_MINUS:
                keys[KEY_MINUS] = state;
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

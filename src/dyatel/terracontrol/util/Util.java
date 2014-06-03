package dyatel.terracontrol.util;

import java.util.Random;

public class Util {

    private static Random random = new Random();

    public static Random getRandom() {
        return random;
    }

    public static void updateRandom(String seed) {
        if (seed.equals(""))
            random = new Random();
        else
            random = new Random(seed.hashCode());
    }

}

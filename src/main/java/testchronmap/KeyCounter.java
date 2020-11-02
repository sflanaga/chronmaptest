package testchronmap;

import java.security.Key;
import java.util.Random;

public class KeyCounter implements KeyProducer {
    private final int[] keys;
    private final int top;
    private final int maxCount;
    private int currCount;

    public KeyCounter(int noKeys, int top) {
        this(noKeys, top, 0);
    }

    public KeyCounter(int noKeys, int top, int maxCount) {
        this.keys = new int[noKeys];
        this.top = top;
        for (int i = 0; i < noKeys; i++) {
            this.keys[i] = 0;
        }
        this.maxCount = maxCount;
        this.currCount = 0;
    }

    @Override
    public void reset() {
        for (int i = 0; i < keys.length; i++) {
            keys[i] = 0;
        }
        currCount = 0;
    }

    public int[] randomKey(Random r) {
        var rk = new int[this.keys.length];
        for (int i = 0; i < rk.length; i++) {
            rk[i] = r.nextInt(top);
        }
        return rk;
    }

    @Override
    public int[] next() {
        currCount++;
        if (maxCount > 0 && currCount > maxCount)
            return null;

        boolean incUp = false;
        for (int i = keys.length - 1; i >= 0; i--) {
            if (i == 0 && incUp && keys[i] >= top)
                return null;
            if (keys[i] >= top) {
                incUp = true;
                keys[i] = 0;
            } else {
                keys[i]++;
                break;
            }
        }
        return keys;
    }

}

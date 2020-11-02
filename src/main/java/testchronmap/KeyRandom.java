package testchronmap;

import java.util.Random;

public class KeyRandom implements KeyProducer{

    private final int[] keys;
    private final int top;
    private final int maxCount;
    private int currCount;
    private Random rand;


    public KeyRandom(int noKeys, int top, int maxCount) {
        this.keys = new int[noKeys];
        this.top = top;
        for (int i = 0; i < noKeys; i++) {
            this.keys[i] = 0;
        }
        this.maxCount = maxCount;
        this.currCount = 0;
        this.rand = new Random(5);
    }

    @Override
    public void reset() {
        for (int i = 0; i < keys.length; i++) {
            keys[i] = 0;
        }
        currCount = 0;
        rand.setSeed(5);
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

        return randomKey(this.rand);
    }
}
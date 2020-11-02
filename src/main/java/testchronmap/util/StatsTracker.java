package testchronmap.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class StatsTracker extends Thread {

    public final String nameOfTrack;
    public final long interval;
    private transient long lastPrint = 0;
    public long startTime = 0;
    public StatsTracker(String name, long interval) {
        this.nameOfTrack = name;
        this.interval = interval;
    }
    public static interface Absolute {
        String getAbsolute();
    }

    @Override
    public void start() {
        startTime = System.currentTimeMillis();
        lastPrint = startTime;
        if ( interval > 0)
            super.start();
    }

    @Override
    public void run() {
        try {
            while (true) {
                long now = System.currentTimeMillis();
                Thread.sleep((now / interval +1) * interval - now);

                printStats(false);
            }
        } catch (InterruptedException e) {
            printStats(true);
            return;
        }
    }

    public void done() {
        try {
            if (interval > 0) {
                this.interrupt();
                this.join();
            }
        } catch(InterruptedException e) {
            throw new RuntimeException("interrupted the interruption of StatsTracker", e);
        }
    }

    public static class Stat {
        public final String name;
        public final AtomicLong val;
        public long lastVal;
        public final String format;

        public Stat(String name, boolean writeAbsVal, boolean writeDiffVal, AtomicLong val) {
            this.name = name;
            this.val = val;
            this.lastVal = val.get();
            var b = new StringBuilder();
            b.append(" [%1$s %2$,.1f/s");
            if (writeDiffVal)
                b.append(" %3$,d");
            if (writeAbsVal)
                b.append(" %4$,d");
            b.append(']');
            this.format = b.toString();
        }

        public long addGet(long val) {
            return this.val.addAndGet(val);
        }
    }

    private final ArrayList<Stat> stats = new ArrayList<>(3);
    private final LinkedHashMap<String, Absolute> absolutes = new LinkedHashMap<>(3);

    public AtomicLong addGetStat(String name, boolean writeAbsVal, boolean writeDiff) {
        var v = new AtomicLong(0);
        var s = new Stat(name, writeAbsVal, writeDiff, v);
        stats.add(s);
        return v;
    }
    public void addCallAbsolute(String name, Absolute abs) {
        absolutes.put(name, abs);
    }

    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    private void printStats(boolean last) {
        long now = System.currentTimeMillis();

        long lastTime = this.lastPrint;
        if (last)
            lastTime = startTime;

        long delta = now - lastTime;

        this.lastPrint = now;
        StringBuilder b = new StringBuilder();
        b.append(nameOfTrack).append(": ");
        b.append(sdf.format(new Date(now)));
        for (var s : stats) {
            long v = s.val.get();
            long d = v - s.lastVal;
            if ( last )
                d = v; // last count the overall time and delta
            s.lastVal = v;
            double r = (d * 1000.0D) / delta;
            b.append(String.format(s.format, s.name, r, d, v));
        }
        for(var e: absolutes.entrySet()) {
            b.append(String.format(" [%s:%s] ", e.getKey(), e.getValue().getAbsolute()));
        }
        if (last) {
//            System.out.println();
            b.append("  -  FINAL (overall) "); // maybe add cpu usage?
        }
        System.out.println(b.toString());
//        if (last)
//            System.out.println();
    }

}

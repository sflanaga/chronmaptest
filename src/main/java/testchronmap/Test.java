package testchronmap;


import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import picocli.CommandLine;
import shaded.org.apache.http.conn.routing.RouteInfo;
import testchronmap.util.StatsTracker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import java.util.stream.IntStream;

public class Test {

    public static class Cli {
        @CommandLine.Option(names = {"-k", "--no-keys"}, arity = "1",
                defaultValue = "4", description = "Number of fields in primary key")
        int no_keys;

        @CommandLine.Option(names = {"-t", "--top-per-key"}, arity = "1",
                defaultValue = "10", description = "Highest value per key")
        int key_top;

        @CommandLine.Option(names = {"--random-keys"}, arity = "1",
                defaultValue = "false", description = "Use random keys instead of incrementals")
        boolean randomKeys;

        @CommandLine.Option(names = {"-n", "--no-records"}, arity = "1",
                defaultValue = "100000", description = "Number of values in record")
        int no_records;

        @CommandLine.Option(names = {"-v", "--no-value"}, arity = "1",
                defaultValue = "10", description = "Number of values in record")
        int no_values;

        @CommandLine.Option(names = {"-b", "--maxbloatfactor"}, arity = "0..1",
                defaultValue = "1.0", description = "Bloat factor - how many times it can expand from original")
        double maxBloatFactor;

        @CommandLine.Option(names = {"-f", "--file"}, arity = "0..1",
                description = "File to memory map too - no file means just in memory")
        Path file;

        @CommandLine.Option(names = {"-r", "--recover"}, arity = "1",
                defaultValue = "false", description = "Use ChronicleMaps recorver mode when opening/creating a mmap file")
        boolean recover;

        @CommandLine.Option(names = {"-i", "--stats-interval"}, arity = "1",
                defaultValue = "1000", description = "Interval in ms between stat update")
        long stats_interval_ms;

        @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
        boolean usageHelpRequested;

    }

    public static ChronicleMap<CharSequence, CharSequence> createMap(Cli cli) {
        try {

        var buf = new StringBuilder();
        for (int i = 0; i < cli.no_values * 2.5; i++) {
            buf.append('9');
        }

        var builder = ChronicleMapBuilder
                .simpleMapOf(CharSequence.class, CharSequence.class)
                .entries(cli.no_records)
                .maxBloatFactor(cli.maxBloatFactor)
                .averageValue(buf.toString());
        if (cli.file != null && cli.recover)
            return builder.createOrRecoverPersistedTo(cli.file.toFile());
        else if (cli.file != null && !cli.recover)
            return builder.createPersistedTo(cli.file.toFile());
        else
            return builder.create();
        } catch(Exception e) {
            throw new RuntimeException("Unable to create chron map: " + e, e);
        }

    }

    public static KeyProducer createKeyProducter(Cli cli) {
        if (cli.randomKeys)
            return new KeyRandom(cli.no_keys, cli.key_top, cli.no_records);
        else
            return new KeyCounter(cli.no_keys, cli.key_top, cli.no_records);
    }

    public static void main(String[] args) {
        final Cli cli = new Cli();
        try {
            var cl = new CommandLine(cli);
            cl.parseArgs(args);
            if (cli.usageHelpRequested) {
                cl.usage(System.err);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        var tracker = new StatsTracker("main", cli.stats_interval_ms);
        var row_count = tracker.addGetStat("rows", true, false);
        var field_count = tracker.addGetStat("fields", true, false);
        var key_dup = tracker.addGetStat("kdups", true, false);
        var val_dif = tracker.addGetStat("valdif", true, false);
        final ChronicleMap<CharSequence, CharSequence> chronMap = createMap(cli);
        var keyProducer = createKeyProducter(cli);
        var key = keyProducer.next();
        try {
            tracker.addCallAbsolute("heap", new StatsTracker.Absolute() {
                @Override public String getAbsolute() {
                    return String.format("%.2f MB", chronMap.offHeapMemoryUsed()/(1024.0D*1024.0D));
                }
            });
            System.out.printf("off heap: %d MB\n", chronMap.offHeapMemoryUsed()/(1024L*1024));

            tracker.start();
            var rand = new Random(5);
            var buf = new StringBuilder();
            var values = new Number[cli.no_values];

            while (key != null) {
                row_count.incrementAndGet();

                buf.setLength(0);
                var keyString = NsvString.joinTo(buf, key).toString();
                for (int i = 0; i < values.length; i++) {
                    values[i] = rand.nextInt(100);
                }
                buf.setLength(0);
                var valueString = NsvString.joinTo(buf, values).toString();
                var currval = chronMap.get(keyString);
                if (currval == null) {
                    chronMap.put(keyString, valueString);
                    field_count.addAndGet(key.length + values.length);
                } else {
                    if (!currval.toString().equals(valueString)) {
                        val_dif.incrementAndGet();
                    }
                    key_dup.incrementAndGet();
                }

                key = keyProducer.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("At key: " + row_count.get());
        } finally {
            if (chronMap != null)
                chronMap.close();
            if (tracker != null)
                tracker.done();
        }
    }
}

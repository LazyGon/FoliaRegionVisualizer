package net.okocraft.foliaregionvisualizer;

import com.flowpowered.math.vector.Vector2d;
import io.papermc.paper.util.CoordinateUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class Benchmark {

    static void benchmark(@NotNull LongSet sections, @NotNull Function<LongSet, List<Vector2d>> algorithm) {
        for (int i = 0; i < 3; i++) {
            long start = System.currentTimeMillis();
            var points = algorithm.apply(sections);
            long end = System.currentTimeMillis();
            System.out.println("temp:" + points.size() + " (took: " + (end - start) + "ms)");
        }

        long took = 0;
        int count = 5;

        for (int i = 0; i < count; i++) {
            long start = System.currentTimeMillis();
            var points = algorithm.apply(sections);
            long end = System.currentTimeMillis();
            took += (end - start);
            System.out.println("temp:" + points.size() + " (took: " + (end - start) + "ms)");
        }

        System.out.println((took / count) + "ms");
    }

    static void exploreLimit(@NotNull Function<LongSet, List<Vector2d>> algorithm) {
        for (int i = 100; i < 1000000; i += 100) {
            var sections = createSections(i);

            long start = System.currentTimeMillis();
            var points = algorithm.apply(sections);
            long end = System.currentTimeMillis();

            int length = i << 1;

            System.out.println(length + "x" + length + ": " + points.size() + " points (took: " + (end - start) + "ms)");
        }
    }

    static LongSet createSections(int radius) {
        int length = radius << 1;
        long size = ((long) length) * length;

        if (Integer.MAX_VALUE < size) {
            throw new IllegalArgumentException("Too big"); // temp
        }

        var sources = new LongOpenHashSet((int) size);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                sources.add(CoordinateUtils.getChunkKey(x, z));
            }
        }

        return sources;
    }

    static LongSet createSections(int radius, int center) {
        int length = radius << 1;
        long size = ((long) length) * length;

        if (Integer.MAX_VALUE < size) {
            throw new IllegalArgumentException("Too big"); // temp
        }

        var sources = new LongOpenHashSet((int) size);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                sources.add(CoordinateUtils.getChunkKey(x + center, z + center));
            }
        }

        return sources;
    }

    static LongSet createSectionsWithHole(int radius) {
        var sources = createSections(radius);

        int start = radius >> 1;

        for (int x = -start; x <= start; x++) {
            for (int z = -start; z <= start; z++) {
                sources.remove(CoordinateUtils.getChunkKey(x, z));
            }
        }

        return sources;
    }

    static LongSet createWTFSections(int radius) {
        var random = new Random();
        int length = radius << 1;
        long size = ((long) length) * length;

        if (Integer.MAX_VALUE < size) {
            throw new IllegalArgumentException("Too big"); // temp
        }

        var sources = createSections(radius);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (Math.abs(x) == Math.abs(z)) {
                    sources.remove(CoordinateUtils.getChunkKey(x, z));
                }
            }
        }

        if (false)
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (random.nextInt(10) != 0) {
                    sources.add(CoordinateUtils.getChunkKey(x, z));
                } else {
                    sources.remove(CoordinateUtils.getChunkKey(x - 1, z));
                    sources.remove(CoordinateUtils.getChunkKey(x + 1, z));
                    sources.remove(CoordinateUtils.getChunkKey(x, z - 1));
                    sources.remove(CoordinateUtils.getChunkKey(x, z + 1));
                }
            }
        }

        return sources;
    }

    static LongSet createDiamondSections(int radius) {
        var sources = new LongOpenHashSet();

        int zRadius = 0;
        for (int x = -radius; x <= 0; x++) {
            for (int z = 0; z <= zRadius; z++) {
                sources.add(CoordinateUtils.getChunkKey(x, z));
            }
            zRadius++;
        }

        for (long key : sources.toLongArray()) {
            int x = CoordinateUtils.getChunkX(key);
            int z = CoordinateUtils.getChunkZ(key);

            sources.add(CoordinateUtils.getChunkKey(x, z));
            sources.add(CoordinateUtils.getChunkKey(x, -z));
            sources.add(CoordinateUtils.getChunkKey(-x, z));
            sources.add(CoordinateUtils.getChunkKey(-x, -z));
        }

        return sources;
    }
}

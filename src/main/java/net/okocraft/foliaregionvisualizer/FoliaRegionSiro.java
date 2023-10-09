package net.okocraft.foliaregionvisualizer;

import com.flowpowered.math.vector.Vector2d;
import io.papermc.paper.util.CoordinateUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FoliaRegionSiro {

    static List<Vector2d> merge(@NotNull LongSet sections) {
        var sectionKeys = new LongOpenHashSet();
        var first = new Line().start(Integer.MAX_VALUE, Integer.MAX_VALUE);

        sections.forEach(sectionKey -> {
            int x1 = CoordinateUtils.getChunkX(sectionKey);
            int z1 = CoordinateUtils.getChunkZ(sectionKey);

            if (hasSectionsOnAllSides(sections, x1, z1)) {
                return;
            }

            int x2 = x1 + 1;
            int z2 = z1 + 1;

            sectionKeys.add(CoordinateUtils.getChunkKey(x1, z1));
            sectionKeys.add(CoordinateUtils.getChunkKey(x1, z2));
            sectionKeys.add(CoordinateUtils.getChunkKey(x2, z1));
            sectionKeys.add(CoordinateUtils.getChunkKey(x2, z2));

            if (x1 < first.startX()) {
                first.startX(x1);
            }

            if (z1 < first.startZ()) {
                first.startZ(z1);
                first.endZ(z1); // z is fixed when finding the end point of the first line
            }
        });

        findFirstLine(sectionKeys, first);

        var currentLine = new Line().start(first.endX(), first.endZ()).end(first.endX(), first.endZ() + 1);

        var points = new ArrayList<Vector2d>();
        points.add(new Vector2d(first.endX() << 8, first.endZ() << 8));
        int direction = 2; // top to bottom

        while (!currentLine.isConnectedTo(first)) {
            int x = currentLine.endX();
            int z = currentLine.endZ();
            long nextSec = leftSideSectionKey(direction, x, z);

            if (sectionKeys.contains(nextSec)) {
                points.add(new Vector2d(x << 8, z << 8));
                currentLine.start(x, z).end(CoordinateUtils.getChunkX(nextSec), CoordinateUtils.getChunkZ(nextSec));
                direction = turnLeft(direction);
                continue;
            }

            nextSec = forwardSectionKey(direction, x, z);

            if (sectionKeys.contains(nextSec)) {
                currentLine.end(CoordinateUtils.getChunkX(nextSec), CoordinateUtils.getChunkZ(nextSec));
                continue;
            }

            nextSec = rightSideSectionKey(direction, x, z);

            points.add(new Vector2d(x << 8, z << 8));
            currentLine.start(x, z).end(CoordinateUtils.getChunkX(nextSec), CoordinateUtils.getChunkZ(nextSec));
            direction = turnRight(direction);
        }

        points.add(new Vector2d(currentLine.endX() << 8, currentLine.endZ() << 8));

        return points;
    }

    private static boolean hasSectionsOnAllSides(@NotNull LongSet sections, int x, int z) {
        return checkSection(sections, x - 1, z) && checkSection(sections, x, z - 1) &&
                checkSection(sections, x + 1, z) && checkSection(sections, x, z + 1);
    }

    private static boolean checkSection(@NotNull LongSet sections, int x, int z) {
        return sections.contains(CoordinateUtils.getChunkKey(x, z));
    }

    private static void findFirstLine(@NotNull LongSet sectionKeys, @NotNull Line firstLine) {
        long zKey = ((long) firstLine.startZ()) << 32;
        boolean firstPointFound = false;

        for (int x = firstLine.startX(); ; x++) {
            if (sectionKeys.contains(zKey | (x & 0xFFFFFFFFL))) {
                if (firstPointFound) {
                    firstLine.endX(x);
                } else {
                    firstPointFound = true;
                    firstLine.startX(x);
                    firstLine.endX(x + 1);
                }
            } else {
                if (firstPointFound) {
                    break;
                }
            }
        }
    }

    //     0
    //     ↑
    //  3 ← → 1
    //     ↓
    //     2

    private static int turnLeft(int direction) {
        return direction + 3 & 3;
    }

    private static int turnRight(int direction) {
        return direction + 1 & 3;
    }

    private static long leftSideSectionKey(int direction, int x, int z) {
    /*
        if (direction == TOP_TO_BOTTOM) { // 2 0b10
            return CoordinateUtils.getChunkKey(x + 1, z);
        } else if (direction == BOTTOM_TO_TOP) { // 0 0b00
            return CoordinateUtils.getChunkKey(x - 1, z);
        } else if (direction == LEFT_TO_RIGHT) { // 1 0b01
            return CoordinateUtils.getChunkKey(x, z - 1);
        } else /* if (direction == RIGHT_TO_LEFT) / { // 3 0b11
            return CoordinateUtils.getChunkKey(x, z + 1);
        }
    */
        if ((direction & 1) == 0) {
            return CoordinateUtils.getChunkKey((direction & 2) == 0 ? x - 1 : x + 1, z);
        } else {
            return CoordinateUtils.getChunkKey(x, (direction & 2) == 0 ? z - 1 : z + 1);
        }
    }

    private static long rightSideSectionKey(int direction, int x, int z) {
        if ((direction & 1) == 0) {
            return CoordinateUtils.getChunkKey((direction & 2) == 0 ? x + 1 : x - 1, z);
        } else {
            return CoordinateUtils.getChunkKey(x, (direction & 2) == 0 ? z + 1 : z - 1);
        }
    }

    private static long forwardSectionKey(int direction, int x, int z) {
        if ((direction & 1) == 0) {
            return CoordinateUtils.getChunkKey(x, (direction & 2) == 0 ? z - 1 : z + 1);
        } else {
            return CoordinateUtils.getChunkKey((direction & 2) == 0 ? x + 1 : x - 1, z);
        }
    }
}

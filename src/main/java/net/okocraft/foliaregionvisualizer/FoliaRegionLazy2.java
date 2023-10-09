package net.okocraft.foliaregionvisualizer;

import com.flowpowered.math.vector.Vector2d;
import io.papermc.paper.util.CoordinateUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

class FoliaRegionLazy2 {

    static List<Vector2d> merge(LongSet sections) {
        var res = withHalls(sortLines(toOutline(sections)));
        List<Vector2d> result = new ArrayList<>(res.size());

        for (Line line : res) {
            result.add(new Vector2d(line.startX() << 8, line.startZ() << 8));

        }

        return result;
    }

    private static ObjectSet<Line> toOutline(LongSet sections) {
        ObjectSet<Line> res = new ObjectOpenHashSet<>();
        var mutableLine = new Line();

        sections.forEach(sectionKey -> {
            int x1 = CoordinateUtils.getChunkX(sectionKey);
            int z1 = CoordinateUtils.getChunkZ(sectionKey);
            int x2 = x1 + 1;
            int z2 = z1 + 1;

            mutableLine.set(x2, z1, x1, z1);
            if (!res.remove(mutableLine)) res.add(mutableLine.createReversed());
            mutableLine.set(x2, z2, x2, z1);
            if (!res.remove(mutableLine)) res.add(mutableLine.createReversed());
            mutableLine.set(x1, z2, x2, z2);
            if (!res.remove(mutableLine)) res.add(mutableLine.createReversed());
            mutableLine.set(x1, z1, x1, z2);
            if (!res.remove(mutableLine)) res.add(mutableLine.createReversed());
        });

        return res;
    }

    private static List<List<Line>> sortLines(Set<Line> lines) {
        List<List<Line>> closedLines = new ArrayList<>();

        boolean outline = true;

        while (!lines.isEmpty()) {

            List<Line> closedLine = new ArrayList<>();

            Line line = null;

            while (line == null || !line.canConnectTo(closedLine.get(0))) {
                if (line == null) {
                    line = lines.stream()
                            .min(outline
                                    ? Comparator.<Line>comparingInt(l -> l.startZ()).thenComparingInt(l -> l.startX())
                                    : Comparator.<Line>comparingInt(l -> l.startX()).thenComparingInt(l -> -l.startZ())
                            )
                            .orElseThrow();
                } else {
                    Line next = line.createNext();
                    boolean clockwise = outline
                            ? !lines.contains(next.createReversed())
                            : lines.contains(next.createReversed());

                    line = next.createRotated(clockwise);
                    if (!lines.contains(line)) {
                        line = next.createRotated(!clockwise);
                    }
                }

                closedLine.add(line);
                lines.remove(line);

                for (Line next = line.createNext(); lines.remove(next); next = line.createNext()) {
                    line.connect(next);
                }
            }

            closedLines.add(closedLine);

            if (outline) {
                outline = false;
            }
        }

        closedLines.sort(Comparator.<List<Line>>comparingInt(list -> list.get(0).startZ())
                .thenComparingInt(list -> list.get(0).startX()));

        return closedLines;
    }

    private static List<Line> withHalls(List<List<Line>> sortedLines) {
        List<Line> result = new ArrayList<>(sortedLines.get(0));

        for (List<Line> hall : sortedLines.subList(1, sortedLines.size())) {
            Line firstLine = hall.get(0);
            Line lastLine = hall.get(hall.size() - 1);
            Line upperLine = result.stream()
                    .filter(l -> l.startZ() == l.endZ()
                            && l.startX() <= firstLine.startX() && firstLine.startX() <= l.endX()
                            && l.startZ() < firstLine.startZ()
                    )
                    .max(Comparator.comparingInt(l -> l.startZ()))
                    .orElseThrow();

            if (upperLine.startX() < firstLine.startX() && firstLine.startX() < upperLine.endX()) {
                Line secondHalf = upperLine.divide(firstLine.startX(), upperLine.startZ());
                Line in = secondHalf.createCopy().connect(firstLine);
                lastLine.connect(secondHalf);

                int idx = result.indexOf(upperLine) + 1;
                result.add(idx, secondHalf);
                result.addAll(idx, hall);
                result.add(idx, in);

            } else if (upperLine.startX() == firstLine.startX()) {
                result.get(result.indexOf(upperLine) - 1).connect(firstLine);
                lastLine.connect(upperLine);
                result.addAll(result.indexOf(upperLine), hall);

            } else if (upperLine.endX() == firstLine.startX()) {
                Line in = result.get(result.indexOf(upperLine) + 1);
                lastLine.endZ(in.endZ());
                in.connect(firstLine);
                result.addAll(result.indexOf(in) + 1, hall);
            }
        }

        return result;
    }
}

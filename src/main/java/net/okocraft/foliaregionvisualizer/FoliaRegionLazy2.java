package net.okocraft.foliaregionvisualizer;

import com.flowpowered.math.vector.Vector2d;
import io.papermc.paper.util.CoordinateUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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

        while (!lines.isEmpty()) {

            List<Line> closedLine = new ArrayList<>();

            Line line = null;

            boolean clockwise = false;

            while (line == null || !line.canConnectTo(closedLine.get(0))) {
                if (line == null) {
                    line = lines.stream()
                            .min(Comparator.<Line>comparingInt(Line::startX).thenComparingInt(Line::startZ))
                            .orElseThrow();
                    if (line.startZ() == line.endZ()) {
                        clockwise = true;
                    }
                } else {
                    Line next = line.createNext();
                    boolean rotateClockwise = clockwise != lines.contains(next.createReversed());
                    line = next.createRotated(rotateClockwise);
                    if (!lines.contains(line)) {
                        line = next.createRotated(!rotateClockwise);
                    }
                }

                closedLine.add(line);
                lines.remove(line);

                for (Line next = line.createNext(); lines.remove(next); next = line.createNext()) {
                    line.connect(next);
                }
            }

            closedLines.add(closedLine);
        }

        closedLines.sort(Comparator.<List<Line>>comparingInt(list -> list.get(0).startX())
                .thenComparingInt(list -> list.get(0).startZ()));

        return closedLines;
    }

    private static List<Line> withHalls(List<List<Line>> sortedLines) {
        List<Line> parent = new ArrayList<>(sortedLines.get(0));

        for (List<Line> child : sortedLines.subList(1, sortedLines.size())) {
            Line firstLine = child.get(0);

            Optional<Line> optionalUpperLine = parent.stream()
                    .filter(l -> l.startZ() == l.endZ()
                            && (l.startX() <= firstLine.startX() && firstLine.startX() <= l.endX()
                                    || l.endX() <= firstLine.startX() && firstLine.startX() <= l.startX())
                            && l.startZ() <= firstLine.startZ())
                    .max(Comparator.comparingInt(Line::startZ));

            if (optionalUpperLine.isEmpty()) {
                continue;
            }

            Line lastLine = child.get(child.size() - 1);
            Line upperLine = optionalUpperLine.get();

            if (upperLine.startX() < upperLine.endX()) {

                if (upperLine.startX() < lastLine.endX() && lastLine.endX() < upperLine.endX()) {
                    Line secondHalf = upperLine.divide(lastLine.endX(), upperLine.startZ());
                    Line out = firstLine.createCopy().endZ(secondHalf.endZ());
                    firstLine.startZ(upperLine.endZ());

                    int idx = parent.indexOf(upperLine) + 1;
                    parent.add(idx, secondHalf);
                    parent.add(idx, out);
                    parent.addAll(idx, child);

                } else if (upperLine.startX() == lastLine.endX()) {
                    int idx = parent.indexOf(upperLine);
                    child.add(child.remove(0));
                    parent.get((idx - 1 + parent.size()) % parent.size()).endZ(child.get(0).endZ());
                    firstLine.endZ(upperLine.startZ());
                    parent.addAll(idx, child);

                } else if (upperLine.endX() == lastLine.endX()) {
                    int idx = (parent.indexOf(upperLine) + 1) % parent.size();
                    parent.get(idx).startZ(lastLine.endZ());
                    firstLine.startZ(upperLine.endZ());
                    parent.addAll(idx, child);
                }

            } else {

                if (upperLine.endX() < firstLine.startX() && firstLine.startX() < upperLine.startX()) {
                    Line secondHalf = upperLine.divide(firstLine.startX(), upperLine.startZ());
                    Line in = secondHalf.createCopy().end(firstLine.startX(), firstLine.startZ());
                    lastLine.endZ(upperLine.endZ());

                    int idx = parent.indexOf(upperLine) + 1;
                    parent.add(idx, secondHalf);
                    parent.addAll(idx, child);
                    parent.add(idx, in);

                } else if (upperLine.startX() == firstLine.startX()) {
                    int idx = parent.indexOf(upperLine);
                    parent.get((idx - 1 + parent.size()) % parent.size()).endZ(firstLine.startZ());
                    lastLine.endZ(upperLine.startZ());
                    parent.addAll(idx, child);

                } else if (upperLine.endX() == firstLine.startX()) {
                    int idx = (parent.indexOf(upperLine) + 1) % parent.size();
                    child.add(0, child.remove(child.size() - 1));
                    parent.get(idx).startZ(lastLine.startZ());
                    lastLine.startZ(upperLine.endZ());
                    parent.addAll(idx, child);
                }
            }
        }

        return parent;
    }
}

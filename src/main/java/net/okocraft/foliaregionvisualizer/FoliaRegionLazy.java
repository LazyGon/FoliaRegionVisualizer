package net.okocraft.foliaregionvisualizer;

import ca.spottedleaf.starlight.common.util.CoordinateUtils;
import com.flowpowered.math.vector.Vector2d;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class FoliaRegionLazy {

    static List<Vector2d> merge(LongSet sections) {
        Set<Line> lineSet = new ObjectOpenHashSet<>();

        for (long sectionKey : sections) {
            int sectionX1 = CoordinateUtils.getChunkX(sectionKey);
            int sectionZ1 = CoordinateUtils.getChunkZ(sectionKey);

            int minX = sectionX1 << 8;
            int minZ = sectionZ1 << 8;
            int maxX = (sectionX1 + 1) << 8;
            int maxZ = (sectionZ1 + 1) << 8;

            //  p1  idx0 →  p2
            //
            //   ↑         idx1
            // idx3         ↓
            //
            //  p4  ← idx2  p3

            Line line;
            line = new Line(minX, minZ, maxX, minZ);
            if (!lineSet.remove(line.reversed())) lineSet.add(line);
            line = new Line(maxX, minZ, maxX, maxZ);
            if (!lineSet.remove(line.reversed())) lineSet.add(line);
            line = new Line(maxX, maxZ, minX, maxZ);
            if (!lineSet.remove(line.reversed())) lineSet.add(line);
            line = new Line(minX, maxZ, minX, minZ);
            if (!lineSet.remove(line.reversed())) lineSet.add(line);

            //System.out.println("Section.of(" + sectionX1 + ", " + sectionZ1 + "), ");
        }

        // 一番上の一番左が始点となる線分を配列の先頭に配置するため、始点の座標で線分をソートする。
        Line[] lines = lineSet.stream()
                .sorted(Comparator.<Line>comparingInt(a -> a.endZ).thenComparingInt(a -> a.endX))
                .toArray(Line[]::new);

        // それぞれの線分の終点から始まる線分を、次の線分にし続けてソートする (LinkedListのイメージ)
        for (int i = 0; i < lines.length - 1; i++) {
            // iの線分の終点から始まる線分の検索
            int candidate1Index = -1;
            int candidate2Index = -1;
            for (int j = i + 1; j < lines.length; j++) {
                if (i == j || lines[i] == null || lines[j] == null) {
                    continue;
                }
                if (lines[i].canConnectTo(lines[j])) {
                    if (candidate1Index == -1) {
                        candidate1Index = j;
                    } else {
                        candidate2Index = j;
                        break;
                    }
                }
            }

            // iの線分の終点から始まる線分がない
            if (candidate1Index == -1) {
                continue;
            }

            // iの線分の終点から始まる線分が一意である
            if (candidate2Index == -1) {
                Line candidate1 = lines[candidate1Index];
                lines[candidate1Index] = lines[i + 1];
                lines[i + 1] = candidate1;
                // iの線分の終点から始まる線分が2つ以上ある
            } else {
                Line candidate1 = lines[candidate1Index];
                Line candidate2 = lines[candidate2Index];
                // 左方向へ進行
                if (lines[i].cross(candidate1) < 0) {
                    lines[candidate1Index] = lines[i + 1];
                    lines[i + 1] = candidate1;
                } else {
                    lines[candidate2Index] = lines[i + 1];
                    lines[i + 1] = candidate2;
                }
            }

            if (lines[i + 1].canConnectTo(lines[0])) {
                for (int j = i + 2; j < lines.length; j++) {
                    lines[j] = null;
                }
                break;
            }
        }

        // 中間点削除
        for (int i = 0; i < lines.length; i++) {
            // 次の点を検索し、存在した場合は中間点を削除する
            for (int j = i + 1; j < lines.length; j++) {
                if (lines[i] == null || lines[j] == null) {
                    continue;
                }
                if (lines[i].canConnectTo(lines[j]) && lines[i].isParallelLine(lines[j])) {
                    lines[i] = lines[i].connect(lines[j]);
                    lines[j] = null;
                    if (lines[i].canConnectTo(lines[0])) {
                        lines[i] = null;
                    } else {
                        i--;
                    }
                    break;
                }
            }
        }

        List<Vector2d> result = new ArrayList<>();

        for (Line line : lines) {
            if (line != null) {
                result.add(new Vector2d(line.endX, line.endZ));
            }
        }

        return result;
    }

    private record Line(int startX, int startZ, int endX, int endZ) {

        private boolean canConnectTo(Line other) {
            return other.startX == this.endX && other.startZ == this.endZ;
        }

        private Line connect(Line other) {
            return new Line(startX, startZ, other.endX, other.endZ);
        }

        private boolean isParallelLine(Line other) {
            return cross(other) == 0;
        }

        private int cross(Line other) {
            return (this.endX - this.startX) * (other.endZ - other.startZ) -
                    (this.endZ - this.startZ) * (other.endX - other.startX);
        }

        private Line reversed() {
            return new Line(endX, endZ, startX, startZ);
        }
    }
}

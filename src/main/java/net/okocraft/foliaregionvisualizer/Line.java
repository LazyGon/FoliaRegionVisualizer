package net.okocraft.foliaregionvisualizer;

import org.jetbrains.annotations.NotNull;

class Line {

    private int startX, startZ, endX, endZ;

    Line() {
    }

    Line(int startX, int startZ, int endX, int endZ) {
        this.startX = startX;
        this.startZ = startZ;
        this.endX = endX;
        this.endZ = endZ;
    }

    public int startX() {
        return startX;
    }

    public @NotNull Line startX(int startX) {
        this.startX = startX;
        return this;
    }

    public int startZ() {
        return startZ;
    }

    public @NotNull Line startZ(int startZ) {
        this.startZ = startZ;
        return this;
    }

    public int endX() {
        return endX;
    }

    public @NotNull Line endX(int endX) {
        this.endX = endX;
        return this;
    }

    public int endZ() {
        return endZ;
    }

    public @NotNull Line endZ(int endZ) {
        this.endZ = endZ;
        return this;
    }

    public @NotNull Line start(int x, int z) {
        this.startX = x;
        this.startZ = z;
        return this;
    }

    public @NotNull Line end(int x, int z) {
        this.endX = x;
        this.endZ = z;
        return this;
    }

    public @NotNull Line set(int startX, int startZ, int endX, int endZ) {
        this.startX = startX;
        this.startZ = startZ;
        this.endX = endX;
        this.endZ = endZ;
        return this;
    }

    public boolean canConnectTo(Line other) {
        return other.startX == this.endX && other.startZ == this.endZ;
    }

    public Line connect(Line other) {
        this.endX = other.endX;
        this.endZ = other.endZ;
        return this;
        // return new Line(startX, startZ, other.endX, other.endZ);
    }

    public Line createReversed() {
        return new Line(endX, endZ, startX, startZ);
    }

    public Line createNext() {
        int newEndX = endX;
        int newEndZ = endZ;

        if (endX > startX) {
            newEndX += 1;
        } else if (endX < startX) {
            newEndX -= 1;
        }

        if (endZ > startZ) {
            newEndZ += 1;
        } else if (endZ < startZ) {
            newEndZ -= 1;
        }

        return new Line(endX, endZ, newEndX, newEndZ);
    }

    public Line createCopy() {
        return new Line(startX, startZ, endX, endZ);
    }

    public Line createRotated(boolean outline) {
        return outline
                ? new Line(startX, startZ, startX - (endZ - startZ), startZ + (endX - startX)) // clockwise
                : new Line(startX, startZ, startX + (endZ - startZ), startZ - (endX - startX)); // anticlockwise
    }

    public Line divide(int x, int z) {
        Line other = createCopy();
        this.endX = x;
        this.endZ = z;
        other.startX = x;
        other.startZ = z;
        return other;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Line line = (Line) object;
        return startX == line.startX && startZ == line.startZ && endX == line.endX && endZ == line.endZ;
    }

    @Override
    public int hashCode() {
        int result = 1;

        result = 31 * result + startX;
        result = 31 * result + startZ;
        result = 31 * result + endX;
        result = 31 * result + endZ;

        return result;
    }

    @Override
    public String toString() {
        return "Line{" +
                "startX=" + startX +
                ", startZ=" + startZ +
                ", endX=" + endX +
                ", endZ=" + endZ +
                '}';
    }
}

package com.pro0101.vaultline.modules.stashorganizer;

public final class Pos {
    public int x;
    public int y;
    public int z;

    public Pos() {
    }

    public Pos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Pos otherPos)) return false;
        return x == otherPos.x && y == otherPos.y && z == otherPos.z;
    }

    @Override
    public int hashCode() {
        return (x * 31 + y) * 31 + z;
    }
}

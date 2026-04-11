package brut.util;

import java.util.Objects;

public final class Pair<L, R> {

    private final L mLeft;
    private final R mRight;

    public Pair(L left, R right) {
        mLeft = left;
        mRight = right;
    }

    public L getLeft() {
        return mLeft;
    }

    public R getRight() {
        return mRight;
    }

    public static <A, B> Pair<A, B> of(A left, B right) {
        return new Pair<>(left, right);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Pair) {
            Pair<?, ?> other = (Pair<?, ?>) obj;
            return Objects.equals(mLeft, other.mLeft) && Objects.equals(mRight, other.mRight);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(mLeft) + Objects.hashCode(mRight);
    }

    @Override
    public String toString() {
        return "(" + mLeft + ", " + mRight + ")";
    }
}

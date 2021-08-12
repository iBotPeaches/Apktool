package org.jf.dexlib2.builder;

import javax.annotation.Nullable;

public abstract class ItemWithLocation {
    @Nullable
    MethodLocation location;

    public boolean isPlaced() {
        return location != null;
    }

    public void setLocation(MethodLocation methodLocation) {
        location = methodLocation;
    }
}

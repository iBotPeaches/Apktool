package org.jf.dexlib2.builder;

public class LocatedLabels extends LocatedItems<Label> {
    @Override
    protected String getAddLocatedItemError() {
        return "Cannot add a label that is already placed." +
                "You must remove it from its current location first.";
    }
}

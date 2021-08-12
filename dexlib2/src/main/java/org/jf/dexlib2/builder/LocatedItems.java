package org.jf.dexlib2.builder;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public abstract class LocatedItems<T extends ItemWithLocation> {
    // We end up creating and keeping around a *lot* of MethodLocation objects
    // when building a new dex file, so it's worth the trouble of lazily creating
    // the labels and debugItems lists only when they are needed
    @Nullable
    private List<T> items = null;

    @Nonnull
    private List<T> getItems() {
        if (items == null) {
            return ImmutableList.of();
        }
        return items;
    }

    public Set<T> getModifiableItems(MethodLocation newItemsLocation) {
        return new AbstractSet<T>() {
            @Nonnull
            @Override
            public Iterator<T> iterator() {
                final Iterator<T> it = getItems().iterator();

                return new Iterator<T>() {
                    private @Nullable
                    T currentItem = null;

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public T next() {
                        currentItem = it.next();
                        return currentItem;
                    }

                    @Override
                    public void remove() {
                        if (currentItem != null) {
                            currentItem.setLocation(null);
                        }
                        it.remove();
                    }
                };
            }

            @Override
            public int size() {
                return getItems().size();
            }

            @Override
            public boolean add(@Nonnull T item) {
                if (item.isPlaced()) {
                    throw new IllegalArgumentException(getAddLocatedItemError());
                }
                item.setLocation(newItemsLocation);
                addItem(item);
                return true;
            }
        };
    }

    private void addItem(@Nonnull T item) {
        if (items == null) {
            items = new ArrayList<>(1);
        }
        items.add(item);
    }

    protected abstract String getAddLocatedItemError();

    public void mergeItemsIntoNext(@Nonnull MethodLocation nextLocation, LocatedItems<T> otherLocatedItems) {
        if (otherLocatedItems == this) {
            return;
        }
        if (items != null) {
            for (T item : items) {
                item.setLocation(nextLocation);
            }
            List<T> mergedItems = items;
            mergedItems.addAll(otherLocatedItems.getItems());
            otherLocatedItems.items = mergedItems;
            items = null;
        }
    }
}

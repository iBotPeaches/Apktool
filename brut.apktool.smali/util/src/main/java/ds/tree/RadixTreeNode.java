/*
The MIT License

Copyright (c) 2008 Tahseen Ur Rehman

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package ds.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node of a Radix tree {@link RadixTreeImpl}
 *
 * @author Tahseen Ur Rehman
 * @email tahseen.ur.rehman {at.spam.me.not} gmail.com
 * @param <T>
 */
class RadixTreeNode<T> {
    private String key;

    private List<RadixTreeNode<T>> childern;

    private boolean real;

    private T value;

    /**
     * intailize the fields with default values to avoid null reference checks
     * all over the places
     */
    public RadixTreeNode() {
        key = "";
        childern = new ArrayList<RadixTreeNode<T>>();
        real = false;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T data) {
        this.value = data;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String value) {
        this.key = value;
    }

    public boolean isReal() {
        return real;
    }

    public void setReal(boolean datanode) {
        this.real = datanode;
    }

    public List<RadixTreeNode<T>> getChildern() {
        return childern;
    }

    public void setChildern(List<RadixTreeNode<T>> childern) {
        this.childern = childern;
    }

    public int getNumberOfMatchingCharacters(String key) {
        int numberOfMatchingCharacters = 0;
        while (numberOfMatchingCharacters < key.length() && numberOfMatchingCharacters < this.getKey().length()) {
            if (key.charAt(numberOfMatchingCharacters) != this.getKey().charAt(numberOfMatchingCharacters)) {
                break;
            }
            numberOfMatchingCharacters++;
        }
        return numberOfMatchingCharacters;
    }

    @Override
    public String toString() {
        return key;
    }
}

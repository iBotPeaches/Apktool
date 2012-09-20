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

/**
 * This interface represent the operation of a radix tree. A radix tree,
 * Patricia trie/tree, or crit bit tree is a specialized set data structure
 * based on the trie that is used to store a set of strings. In contrast with a
 * regular trie, the edges of a Patricia trie are labelled with sequences of
 * characters rather than with single characters. These can be strings of
 * characters, bit strings such as integers or IP addresses, or generally
 * arbitrary sequences of objects in lexicographical order. Sometimes the names
 * radix tree and crit bit tree are only applied to trees storing integers and
 * Patricia trie is retained for more general inputs, but the structure works
 * the same way in all cases.
 *
 * @author Tahseen Ur Rehman
 * email: tahseen.ur.rehman {at.spam.me.not} gmail.com
 */
public interface RadixTree<T> {
    /**
     * Insert a new string key and its value to the tree.
     *
     * @param key
     *            The string key of the object
     * @param value
     *            The value that need to be stored corresponding to the given
     *            key.
     * @throws DuplicateKeyException
     */
    public void insert(String key, T value);

    /**
     * Delete a key and its associated value from the tree.
     * @param key The key of the node that need to be deleted
     * @return
     */
    public boolean delete(String key);

    /**
     * Find a value based on its corresponding key.
     *
     * @param key The key for which to search the tree.
     * @return The value corresponding to the key. null if iot can not find the key
     */
    public T find(String key);

    /**
     * Find an existing entry and replace it's value. If no existing entry, do nothing
     *
     * @param key The key for which to search the tree.
     * @param value The value to set for the entry
     * @return true if an entry was found for the given key, false if not found
     */
    public boolean replace(String key, final T value);

    /**
     * Check if the tree contains any entry corresponding to the given key.
     *
     * @param key The key that needto be searched in the tree.
     * @return retun true if the key is present in the tree otherwise false
     */
    public boolean contains(String key);

    /**
     * Search for all the keys that start with given prefix. limiting the results based on the supplied limit.
     *
     * @param prefix The prefix for which keys need to be search
     * @param recordLimit The limit for the results
     * @return The list of values those key start with the given prefix
     */
    public ArrayList<T> searchPrefix(String prefix, int recordLimit);

    /**
     * Return the size of the Radix tree
     * @return the size of the tree
     */
    public long getSize();

    /**
     * Complete the a prefix to the point where ambiguity starts.
     *
     *  Example:
     *  If a tree contain "blah1", "blah2"
     *  complete("b") -> return "blah"
     *
     * @param prefix The prefix we want to complete
     * @return The unambiguous completion of the string.
     */
    public String complete(String prefix);
}

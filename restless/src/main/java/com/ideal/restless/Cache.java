/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Ideal Technologies Ltd.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package com.ideal.restless;


import java.io.IOException;

/**
 * A generic cache interface for storing java object.
 * <p>
 * <em>Note:</em> The {@link Cache} is accessed by multiple threads. Implementation class must ensure
 * thread safe for any cache operation.
 */

public interface Cache<K, V> {

    /**
     * Retrieve an object for the specified {@code key}
     *
     * @param key The key to retrieve the stored object.
     * @return The stored object.
     */
    V get(K key);

    /**
     * Store an object with type {@link V} in the cache for the specified key
     *
     * @param key   The key for the stored object.
     * @param value The stored value.
     */
    void put(K key, V value);

    /**
     * Clears the cache
     */
    void clear() throws IOException;

}

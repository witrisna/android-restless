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

import android.net.http.HttpResponseCache;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.runner.RunWith;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class FileCacheTest extends AbstractCacheTest {

    @Override
    public Cache getCache() throws IOException {
        return new FileCache(InstrumentationRegistry.getTargetContext().getCacheDir(), 1024 * 1024);
    }

    @Override
    public void testCacheHit() throws Exception {
        super.testCacheHit();
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        assertEquals(cache.getHitCount(), 1);
    }

    @Override
    public void testCacheHitWithETagAndExpired() throws Exception {
        super.testCacheHitWithETagAndExpired();
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        assertEquals(cache.getHitCount(), 1);
    }

}

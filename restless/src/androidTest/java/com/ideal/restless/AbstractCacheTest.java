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

import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;

import okhttp3.mockwebserver.RecordedRequest;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public abstract class AbstractCacheTest extends BaseTest {

    protected RequestThreadPool requestThreadPool;

    @Before
    public void before() throws Exception {

        RequestThreadPool.Builder builder = new RequestThreadPool.Builder();
        requestThreadPool = builder.setCorePoolSize(1)
                .setCache(getCache())
                .build();
    }

    @After
    public void after() throws Exception {
        requestThreadPool.shutdown();
        getCache().clear();
    }

    public abstract Cache getCache() throws IOException;

    @Test
    public void testCacheHit() throws Exception {

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath(DefaultDispatcher.TEST_CACHE.substring(1)).build();
        Request request = new Request.Builder<JSONObject>(uri).onSuccess(new Request.SuccessCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject body, HttpResponse response) {
                countDownLatch.countDown();
            }
        }).cacheable().build();
        requestThreadPool.execute(request);
        await(countDownLatch);


        final CountDownLatch countDownLatch2 = new CountDownLatch(1);
        Request request2 = new Request.Builder<JSONObject>(uri).onSuccess(new Request.SuccessCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject body, HttpResponse response) {
                countDownLatch2.countDown();
            }
        }).cacheable().build();
        requestThreadPool.execute(request2);
        await(countDownLatch2);
        assertEquals(1, webServer.getRequestCount());

    }

    @Test
    public void testCacheHitWithETagAndExpired() throws Exception {

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath(DefaultDispatcher.TEST_CACHE_ETAG.substring(1)).build();
        Request request = new Request.Builder<JSONObject>(uri).onSuccess(new Request.SuccessCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject body, HttpResponse response) {
                countDownLatch.countDown();
            }
        }).cacheable().build();
        requestThreadPool.execute(request);
        await(countDownLatch);

        //Wait for cache expired
        Thread.sleep(1000);

        final CountDownLatch countDownLatch2 = new CountDownLatch(1);
        Request request2 = new Request.Builder<JSONObject>(uri).onSuccess(new Request.SuccessCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject body, HttpResponse response) {
                countDownLatch2.countDown();
            }
        }).cacheable().build();
        requestThreadPool.execute(request2);
        await(countDownLatch2);
        webServer.takeRequest();
        RecordedRequest recordedRequestWithETag = webServer.takeRequest();
        assertNotNull(recordedRequestWithETag.getHeader("If-None-Match"));
        assertEquals(2, webServer.getRequestCount());

    }

    @Test
    public void testCacheExpired() throws Exception {

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath(DefaultDispatcher.TEST_CACHE_EXPIRED.substring(1)).build();
        Request request = new Request.Builder<JSONObject>(uri).onSuccess(new Request.SuccessCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject body, HttpResponse response) {
                countDownLatch.countDown();
            }
        }).cacheable().build();
        requestThreadPool.execute(request);
        await(countDownLatch);

        //Wait for cache expired
        Thread.sleep(1000);

        final CountDownLatch countDownLatch2 = new CountDownLatch(1);
        Request request2 = new Request.Builder<JSONObject>(uri).onSuccess(new Request.SuccessCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject body, HttpResponse response) {
                countDownLatch2.countDown();
            }
        }).cacheable().build();
        requestThreadPool.execute(request2);
        await(countDownLatch2);
        webServer.takeRequest();
        assertEquals(2, webServer.getRequestCount());

    }
}

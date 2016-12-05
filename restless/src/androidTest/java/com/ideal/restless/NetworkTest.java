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
import android.os.Looper;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.RecordedRequest;

@RunWith(AndroidJUnit4.class)
public class NetworkTest extends BaseTest {

    private RequestThreadPool requestThreadPool;

    @Before
    public void before() throws Exception {
        RequestThreadPool.Builder builder = new RequestThreadPool.Builder();
        requestThreadPool = builder.build();
    }

    @After
    public void after() throws Exception {
        requestThreadPool.shutdown();
    }

    @Test
    public void testSuccessCallbackTest() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("test").build();
        Request.Builder<JSONObject> builder = new Request.Builder<JSONObject>(uri);
        final boolean[] result = {false};
        Request request = builder.onSuccess(new Request.SuccessCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject body, HttpResponse response) {
                result[0] = true;
                countDownLatch.countDown();
            }
        }).build();

        requestThreadPool.execute(request);
        await(countDownLatch);
    }

    @Test
    public void testErrorCallbackTest() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("invalidUri").build();
        Request.Builder<JSONObject> builder = new Request.Builder<JSONObject>(uri);
        final boolean[] result = {false};
        final Request request = builder.onError(new Request.ErrorCallback() {
            @Override
            public void onError(Throwable t, HttpResponse response) {
                if (response.getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    result[0] = true;
                }
                countDownLatch.countDown();
            }
        }).build();

        requestThreadPool.execute(request);
        await(countDownLatch);
    }


    @Test
    public void testMainThread() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("test").build();
        Request.Builder<JSONObject> builder = new Request.Builder<JSONObject>(uri);
        final boolean[] result = {false};
        Request request = builder.onSuccess(new Request.SuccessCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject body, HttpResponse response) {
                if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                    result[0] = true;
                }
                countDownLatch.countDown();
            }
        }).build();

        requestThreadPool.execute(request);

        await(countDownLatch);
        assertTrue(result[0]);
    }

    @Test
    public void testWorkerThread() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("test").build();
        Request.Builder<JSONObject> builder = new Request.Builder<JSONObject>(uri);
        final boolean[] result = {false};
        Request request = builder.onSuccess(new Request.SuccessCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject body, HttpResponse response) {
                if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
                    result[0] = true;
                }
                countDownLatch.countDown();
            }
        }).responseOnBackgroundThread().build();

        requestThreadPool.execute(request);

        await(countDownLatch);
        assertTrue(result[0]);
    }



    @Test
    public void testRetry() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("invalidUri").build();
        Request.Builder<JSONObject> builder = new Request.Builder<JSONObject>(uri);
        final boolean[] result = {false};
        final Request request = builder.onError(new Request.ErrorCallback() {
            @Override
            public void onError(Throwable t, HttpResponse response) {
                if (t instanceof FileNotFoundException) {
                    result[0] = true;
                }
                countDownLatch.countDown();
            }
        }).retry(3).build();
        requestThreadPool.execute(request);
        await(countDownLatch);
        assertEquals(3, webServer.getRequestCount());
        assertTrue(result[0]);

    }

    @Test
    public void testJSONPost() throws Exception {
        JSONObject expect = new JSONObject("{\"name\":\"value\"}");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("test").build();
        Request.Builder<JSONObject> builder = new Request.Builder<JSONObject>(uri);
        final boolean[] result = {false};
        Request request = builder.onSuccess(new Request.SuccessCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject body, HttpResponse response) {
                result[0] = true;
                countDownLatch.countDown();
            }
        }).post(expect).build();

        requestThreadPool.execute(request);
        await(countDownLatch);
        assertEquals(expect.toString(), webServer.takeRequest().getBody().readUtf8());
    }

    @Test
    public void testJSONArray() throws Exception {
        JSONArray expect = new JSONArray("[{\"name\":\"value\"}]");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("test_json_array").build();
        Request.Builder<JSONArray> builder = new Request.Builder<JSONArray>(uri);
        final boolean[] result = {false};
        Request request = builder.onSuccess(new Request.SuccessCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray body, HttpResponse response) {
                result[0] = true;
                countDownLatch.countDown();
            }
        }).post(expect).build();

        requestThreadPool.execute(request);
        await(countDownLatch);
        assertEquals(expect.toString(), webServer.takeRequest().getBody().readUtf8());
    }

    @Test
    public void testFormPost() throws Exception {
        LinkedHashMap<String, String> expect = new LinkedHashMap<>();
        expect.put("key1", "value1");
        expect.put("key2", "value2");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("test").build();
        Request.Builder<JSONObject> builder = new Request.Builder<JSONObject>(uri);
        final boolean[] result = {false};
        Request request = builder.onSuccess(new Request.SuccessCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject body, HttpResponse response) {
                result[0] = true;
                countDownLatch.countDown();
            }
        }).post(expect).build();

        requestThreadPool.execute(request);
        await(countDownLatch);
        assertEquals("key1=value1&key2=value2", webServer.takeRequest().getBody().readUtf8());
    }


    @Test
    public void testWithoutCallbackAfterPurge() throws InterruptedException {
        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("slow").build();
        Request.Builder<JSONObject> builder = new Request.Builder<JSONObject>(uri);
        final boolean[] result = {true};
        Request request = builder.onSuccess(new Request.SuccessCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject body, HttpResponse response) {
                result[0] = false;
            }
        }).onError(new Request.ErrorCallback() {
            @Override
            public void onError(Throwable t, HttpResponse httpResponse) {
                result[0] = false;
            }
        }).build();

        requestThreadPool.execute(request);
        Thread.sleep(200);
        requestThreadPool.purge();
        //Wait for the request from the server
        webServer.takeRequest();
        //Make sure callback not invoked
        assertTrue(result[0]);

        //resend request after purge
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Request request2 = builder.onSuccess(new Request.SuccessCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject body, HttpResponse response) {
                result[0] = true;
                countDownLatch.countDown();
            }
        }).onError(new Request.ErrorCallback() {
            @Override
            public void onError(Throwable t, HttpResponse httpResponse) {
                result[0] = true;
                countDownLatch.countDown();
            }
        }).build();

        requestThreadPool.execute(request2);

        await(countDownLatch);
        assertTrue(result[0]);

    }

    @Test
    public void testWithoutExecutetAfterPurge() throws InterruptedException {
        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("slow").build();
        Request.Builder<JSONObject> builder = new Request.Builder<JSONObject>(uri);
        Request request = builder.build();
        requestThreadPool.schedule(request, 300, TimeUnit.MILLISECONDS);
        Thread.sleep(200);
        requestThreadPool.purge();
        RecordedRequest recordedRequest = webServer.takeRequest(500, TimeUnit.MILLISECONDS);
        assertNull(recordedRequest);
    }

    @Test
    public void testTimeout() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("slow").build();
        Request.Builder<JSONObject> builder = new Request.Builder<JSONObject>(uri);
        final boolean[] result = {false};
        final Request request = builder.onError(new Request.ErrorCallback() {
            @Override
            public void onError(Throwable t, HttpResponse response) {
                if (t instanceof SocketTimeoutException ) {
                    result[0] = true;
                }
                countDownLatch.countDown();
            }
        }).timeout(1000).build();

        requestThreadPool.execute(request);
        await(countDownLatch);
        assertTrue(result[0]);
    }

    @Test
    public void testRequestCancel() throws Exception {
        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("slow").build();
        Request.Builder<JSONObject> builder = new Request.Builder<JSONObject>(uri);
        final boolean[] result = {true};
        Request request = builder.onSuccess(new Request.SuccessCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject body, HttpResponse response) {
                result[0] = false;
            }
        }).onError(new Request.ErrorCallback() {
            @Override
            public void onError(Throwable t, HttpResponse httpResponse) {
                result[0] = false;
            }
        }).build();

        requestThreadPool.execute(request);
        Thread.sleep(500);
        request.cancel();
        webServer.takeRequest();

        assertTrue(result[0]);
    }

    @Test
    public void testScheduleRequestCancel() throws Exception {
        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("slow").build();
        Request.Builder<JSONObject> builder = new Request.Builder<JSONObject>(uri);
        final boolean[] result = {true};
        Request request = builder.onSuccess(new Request.SuccessCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject body, HttpResponse response) {
                result[0] = false;
            }
        }).onError(new Request.ErrorCallback() {
            @Override
            public void onError(Throwable t, HttpResponse httpResponse) {
                result[0] = false;
            }
        }).build();

        requestThreadPool.schedule(request, 500, TimeUnit.MILLISECONDS);
        Thread.sleep(200);
        request.cancel();
        RecordedRequest recordedRequest = webServer.takeRequest(1000, TimeUnit.MILLISECONDS);
        assertTrue(result[0]);
        assertNull(recordedRequest);
    }
}

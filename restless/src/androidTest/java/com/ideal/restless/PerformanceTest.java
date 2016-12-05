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
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PerformanceTest extends BaseTest {

    private RequestThreadPool requestThreadPool;
    private RequestQueue requestQueue;

    @Before
    public void before() throws Exception {

        //RequestThreadPool
        RequestThreadPool.Builder builder = new RequestThreadPool.Builder();
        requestThreadPool = builder.setCorePoolSize(10).build();

        //Volley
        requestQueue = Volley.newRequestQueue(InstrumentationRegistry.getInstrumentation().getContext());

    }

    @After
    public void after() throws Exception {
        requestThreadPool.shutdown();
    }


    @Test
    public void testPerformance() throws Exception {

        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("test").build();
        int numberOfRequest = 100;

        final CountDownLatch countDownLatch = new CountDownLatch(numberOfRequest);
        DataMarshaller.JsonDataMarshaller jsonDataMarshaller = new DataMarshaller.JsonDataMarshaller();
        long start = System.currentTimeMillis();
        for (int i = 0; i < numberOfRequest; i++) {
            Request.Builder<JSONObject> builder = new Request.Builder<JSONObject>(uri);
            Request request = builder.onSuccess(new Request.SuccessCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject body, HttpResponse response) {
                    countDownLatch.countDown();
                }
            }).onError(new Request.ErrorCallback() {
                @Override
                public void onError(Throwable t, HttpResponse httpResponse) {
                    countDownLatch.countDown();
                }
            }).setResponseMarshaller(jsonDataMarshaller)
                    .build();
            requestThreadPool.execute(request);
        }
        await(countDownLatch);
        long doneIn = System.currentTimeMillis() - start;
        System.out.println("RequestThreadPool Done in: " + doneIn + "ms");


        final CountDownLatch countDownLatch2 = new CountDownLatch(numberOfRequest);
        long start2 = System.currentTimeMillis();
        String url = uri.toString();
        for (int i = 0; i < numberOfRequest; i++) {
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(com.android.volley.Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    countDownLatch2.countDown();
                }
            }, null);
            requestQueue.add(jsonObjectRequest);
        }
        await(countDownLatch2);
        long doneIn2 = System.currentTimeMillis() - start2;
        System.out.println("RequestQueue Done in: " + doneIn2 + "ms");


        assertTrue(doneIn < doneIn2);
        assertEquals(200, webServer.getRequestCount());


    }
}

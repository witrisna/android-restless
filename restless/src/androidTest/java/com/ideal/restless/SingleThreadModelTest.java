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
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;

@RunWith(AndroidJUnit4.class)
public class SingleThreadModelTest extends BaseTest {

    private static RequestThreadPool requestThreadPool;

    @Before
    public void before() throws Exception {
        RequestThreadPool.Builder builder = new RequestThreadPool.Builder();
        requestThreadPool = builder.setCorePoolSize(1).build();
    }

    @After
    public void after() throws Exception {
        requestThreadPool.shutdown();
    }

    @Test
    public void testInSequence() throws Exception {

        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("test").build();
        final Stack<Integer> stack = new Stack<>();
        int numberOfRequest = 30;
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfRequest);
        for (int i = 0; i < numberOfRequest; i++) {
            Request.Builder<JSONObject> builder = new Request.Builder<JSONObject>(uri);
            final int finalI = i;
            Request request = builder.onSuccess(new Request.SuccessCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject body, HttpResponse response) {
                    stack.push(new Integer(finalI));
                    countDownLatch.countDown();
                }
            }).build();
            requestThreadPool.execute(request);
        }
        await(countDownLatch);

        for (int i = numberOfRequest -1; i >= 0; i--) {
            assertEquals(new Integer(i), stack.pop());
        }
    }

    @Ignore
    public void testRejectExecution() {
        Uri uri = (new Uri.Builder()).scheme("http").encodedAuthority("localhost:" + port).appendPath("test").build();
        int numberOfRequest = 30;
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfRequest);
        final boolean[] result = {false};
        for (int i = 0; i < numberOfRequest; i++) {
            Request.Builder<JSONObject> builder = new Request.Builder<JSONObject>(uri);
            Request request = builder
                    .onSuccess(new Request.SuccessCallback<JSONObject>() {
                        @Override
                        public void onSuccess(JSONObject body, HttpResponse response) {
                            countDownLatch.countDown();
                        }
                    }).onError(new Request.ErrorCallback() {
                        @Override
                        public void onError(Throwable t, HttpResponse httpResponse) {
                            if (t instanceof RejectedExecutionException) {
                                result[0] = true;
                            }
                            countDownLatch.countDown();
                        }
                    }).build();
            requestThreadPool.execute(request);
        }
        await(countDownLatch);
        assertTrue(result[0]);
    }

}

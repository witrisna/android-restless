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

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.Date;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

public class DefaultDispatcher extends Dispatcher {

    public static final String TEST = "/test";
    public static final String TEST_CACHE = "/test_cache";
    public static final String TEST_CACHE_EXPIRED = "/test_cache_expired";
    public static final String TEST_CACHE_ETAG = "/test_cache_etag";
    public static final String TEST_JSON_ARRAY = "/test_json_array";
    public static final String SLOW = "/slow";

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        request.getBody();

        switch (request.getPath()) {
            case TEST:
                return testResponse();
            case TEST_CACHE:
                return testCacheResponse();
            case TEST_CACHE_EXPIRED:
                return testCacheExpiredResponse();
            case TEST_CACHE_ETAG:
                return testCacheResponseWithETag(request);
            case TEST_JSON_ARRAY:
                return testJsonArrayResponse();
            case SLOW:
                Thread.sleep(1000);
                return testResponse();
            default:
                return new MockResponse().setResponseCode(404);
        }
    }

    private MockResponse testCacheResponse() {
        return new MockResponse()
                .addHeader("Cache-Control", "public, max-age=60")
                .addHeader("Content-type", "application/json")
                .setResponseCode(200).setBody(getJsonObject().toString());
    }

    private MockResponse testCacheExpiredResponse() {
        return new MockResponse()
                .addHeader("Cache-Control", "public, max-age=1")
                .addHeader("Content-type", "application/json")
                .setResponseCode(200).setBody(getJsonObject().toString());
    }


    private MockResponse testCacheResponseWithETag(RecordedRequest request) {
        String eTag = request.getHeader("If-None-Match");
        if (eTag == null) {
            return new MockResponse()
                    .addHeader("Cache-Control", "public, max-age=1")
                    .addHeader("ETag", "x12345")
                    .addHeader("Content-type", "application/json")
                    .setResponseCode(200).setBody(getJsonObject().toString());
        } else {
            return new MockResponse()
                    .setResponseCode(HttpURLConnection.HTTP_NOT_MODIFIED);
        }
    }

    private MockResponse testResponse() {
       return new MockResponse()
                .addHeader("Content-type", "application/json")
                .setResponseCode(200).setBody(getJsonObject().toString());
    }

    private MockResponse testJsonArrayResponse() {
        String result = "[{\"key1\":\"dummy\", \"key2\":\"dummy\", \"key3\":" + new Date().getTime() + 36000 + "}]";
        return new MockResponse()
                .addHeader("Content-type", "application/json")
                .setResponseCode(200).setBody(result);
    }

    private JSONObject getJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("key1", "value1");
            jsonObject.put("key2", "value2");
            jsonObject.put("key3", "value3");
            jsonObject.put("key4", "value4");
            jsonObject.put("key5", new Date().getTime());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }


}

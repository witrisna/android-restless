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

import android.net.TrafficStats;
import android.util.Log;


import java.util.Map;

import static com.ideal.restless.RequestThreadPool.DEBUG;
import static com.ideal.restless.RequestThreadPool.TAG;

public class LoggingInterceptor implements RequestInterceptor {

    private static final Integer DURATION = 1;
    private static final Integer TRANSMITTED = 2;
    private static final Integer RECEIVED = 3;

    public LoggingInterceptor() {
    }

    @Override
    public void preExecute(Map requestContext, RequestThreadPool threadPool, Request request) {
        if (DEBUG) {
            int uid = android.os.Process.myUid();
            requestContext.put(RECEIVED, TrafficStats.getUidRxBytes(uid));
            requestContext.put(TRANSMITTED, TrafficStats.getUidTxBytes(uid));
            requestContext.put(DURATION, System.currentTimeMillis());
        }
    }

    @Override
    public void postExecute(Map requestContext, Request request, RequestThreadPool threadPool, Throwable t) {
        if (DEBUG) {
            Long startTime = (Long) requestContext.get(DURATION);
            Long transmitted = (Long) requestContext.get(TRANSMITTED);
            Long received = (Long) requestContext.get(RECEIVED);
            StringBuilder sb = new StringBuilder();
            int uid = android.os.Process.myUid();
            sb.append(request.getUri())
                    .append(" duration=").append(System.currentTimeMillis() - startTime).append("ms")
                    .append(" transmitted=").append(TrafficStats.getUidTxBytes(uid) - transmitted).append("bytes")
                    .append(" received=").append(TrafficStats.getUidRxBytes(uid) - received).append("bytes");
            Log.d(TAG, sb.toString());
        }
    }
}

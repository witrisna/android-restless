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

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

public interface RequestThreadPool {

    String TAG = "HTTP-ThreadPoolExecutor";

    boolean DEBUG = android.util.Log.isLoggable(TAG,  android.util.Log.VERBOSE);

    /**
     * Shutdown the thread poo {@link ThreadPoolExecutor#shutdown()}
     */
    void shutdown();

    /**
     * Wrapper to {@link ScheduledThreadPoolExecutor#schedule(Callable, long, TimeUnit)}
     */
    void schedule(Request request, long delay, TimeUnit timeUnit);

    /**
     * Wrapper to {@link ScheduledThreadPoolExecutor#execute(Runnable)}
     */
    void execute(Request request);

    /**
     * Remove pending request and purge the thread pool. {@link ScheduledThreadPoolExecutor#purge()}
     * The running Request cannot be purged, however the {@link com.ideal.restless.Request.SuccessCallback}
     * and {@link com.ideal.restless.Request.ErrorCallback} will not be called after purge.
     */
    void purge();

    /**
     * @return Return the last purge time.
     */
    long getLastPurgeTime();

    /**
     * @return Return the Cache
     */
    Cache<Request, HttpResponse> getCache();

    /**
     * @return The interceptors to intercept all request assigned to this threadpool
     */
    RequestInterceptor[] getInterceptors();

    /**
     * @return The {@link SSLSocketFactory} to establish the HTTPS Connection
     */
    SSLSocketFactory getSSLSocketFactory();

    class Builder {


        private int corePoolSize = 3;
        private Cache<Request, HttpResponse> cache; //No cache by default
        private RequestInterceptor[] requestInterceptors = new RequestInterceptor[]{
                new ResponseInterceptor(), new CacheRequestInterceptor()};
        private SSLSocketFactory sslSocketFactory;

        /**
         * The Core pool size, refer to {@link ThreadPoolExecutor}
         */
        public Builder setCorePoolSize(int size) {
            this.corePoolSize = size;
            return this;
        }

        /**
         * Set the cache engine to use for {@link RequestThreadPool}
         */
        public Builder setCache(Cache<Request, HttpResponse> cache) {
            this.cache = cache;
            return this;
        }

        /**
         * Set a chain of {@link RequestInterceptor} to intercept the request, by default
         * {@link CacheRequestInterceptor} and {@link ResponseInterceptor} are defined,
         * setting new chain will override the default interceptor chain. To receive callback
         * response, {@link ResponseInterceptor} is required.
         *
         * @param requestInterceptors The interceptors to intercept all request assigned to this threadpool
         */
        public Builder setRequestInterceptors(RequestInterceptor... requestInterceptors) {
            this.requestInterceptors = requestInterceptors;
            return this;
        }

        /**
         * The {@link SSLSocketFactory} to establish the HTTPS connection
         */
        public Builder setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
            this.sslSocketFactory = sslSocketFactory;
            return this;
        }

        public RequestThreadPool build() {


            final RequestThreadPool threadPool = new RequestThreadPool() {

                long lastPurgeTime = 0;

                private ScheduledThreadPoolExecutor pool =
                        new ScheduledThreadPoolExecutor(corePoolSize);


                @Override
                public void shutdown() {
                    pool.shutdown();
                }

                @Override
                public void schedule(Request request, long delay, TimeUnit timeUnit) {
                    RunnableRequest r = (RunnableRequest) request;
                    if (r.shouldDiscard()) {
                        return;
                    }

                    ((RunnableRequest) request).onAttached(this);
                    ((RunnableRequest) request).onAttached(pool.schedule(request, delay, timeUnit));
                }

                @Override
                public synchronized void execute(Request request) {
                    schedule(request, 0, TimeUnit.NANOSECONDS);
                }

                @Override
                public void purge() {
                    lastPurgeTime = System.currentTimeMillis();
                    pool.purge();
                    pool.getQueue().clear();
                }

                @Override
                public long getLastPurgeTime() {
                    return lastPurgeTime;
                }

                @Override
                public Cache<Request, HttpResponse> getCache() {
                    return cache;
                }

                @Override
                public RequestInterceptor[] getInterceptors() {
                    return requestInterceptors;
                }

                @Override
                public SSLSocketFactory getSSLSocketFactory() {
                    return sslSocketFactory;
                }

            };

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                   threadPool.shutdown();
                }
            }));

            return threadPool;

        }
    }


}

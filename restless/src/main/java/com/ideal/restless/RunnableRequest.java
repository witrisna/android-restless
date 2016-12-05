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

import java.net.HttpURLConnection;
import java.util.concurrent.Future;

/**
 * @param <T> The type of parsed response
 */
interface RunnableRequest<T> extends Request {

    /**
     * Trigger the {@link com.ideal.restless.Request.SuccessCallback} which registered by
     * {@link com.ideal.restless.Request.Builder#onSuccess(SuccessCallback)} with the stored
     * {@link HttpResponse}
     */
    void onSuccess() throws Exception;

    /**
     * Trigger the {@link com.ideal.restless.Request.SuccessCallback} which registered by
     * {@link com.ideal.restless.Request.Builder#onSuccess(SuccessCallback)} with the provide
     * {@link HttpResponse}
     *
     * @param response The received HTTP response
     */
    void setResponse(HttpResponse response);

    /**
     * Trigger the {@link com.ideal.restless.Request.ErrorCallback} which registered by
     * {@link com.ideal.restless.Request.Builder#onError(ErrorCallback)} with the provide
     * {@link Throwable}
     */
    void onError(Throwable t);

    /**
     * @return The received HTTP response
     */
    HttpResponse getResponse();

    /**
     * Increment the retry count
     */
    void incrementRetryCount();

    /**
     * Called when the request attached to the threadpool
     *
     * @param threadPool The assigned threadpool
     */
    void onAttached(RequestThreadPool threadPool);

    /**
     * Called when the request attached to the future
     *
     * @param future     Future representing pending completion of the Request
     */
    void onAttached(Future future);

    /**
     * Determine if it should silently discards the request.
     * The request will be discard after invoke {@link Request#cancel()} or
     * {@link RequestThreadPool#purge()}.
     *
     * @return True to discard the request, the {@link com.ideal.restless.Request.SuccessCallback} or
     * {@link com.ideal.restless.Request.ErrorCallback} will not be called.
     */
    boolean shouldDiscard();

    /**
     * The {@link HttpURLConnection} that will be used for this request
     */
    HttpURLConnection getConnection();


    /**
     * @return The Cache Control of the request.
     */
    CacheControl getCacheControl();

}

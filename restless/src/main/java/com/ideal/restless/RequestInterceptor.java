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

import java.util.Map;

/**
 * Interface for general API request interception. Allows for being applied
 * to {@link Request}.
 * <p>
 * <p>The {@link Request} is being executed in a separate thread and the interceptors
 * applied to the {@link Request} will be run on the same thread as the {@link Request} worker thread.
 * <p>
 * <p>Interceptors can be used to add, remove headers, provide analytic, network performance measurement
 * caching, data transformation on the request or response. Two interceptors
 * {@link CacheRequestInterceptor} and {@link ResponseInterceptor} are provided and
 * set to {@link Request} by default.
 */
public interface RequestInterceptor {

    /**
     * Intercept the execution of a API request <em>before</em> its invocation.
     * <p>Allows for modifying the request context before the request execution. (For example, the
     * the request headers)
     * <em>Note:</em> When {@link Exception} is thrown on {@link #preExecute(Map, RequestThreadPool, Request )},
     * the network request, {@link #postExecute(Map, Request, RequestThreadPool, Throwable)} will <em>NOT</em> be executed.
     *
     * @param requestContext The request Context
     * @param threadPool     The thread pool which responsible for the request.
     * @param request        The current API request
     */
    void preExecute(Map requestContext, RequestThreadPool threadPool, Request request );

    /**
     * Intercept the execution of a API request <em>after</em> its successful
     * and failure invocation, right before the thread release the Request.
     * <p>Allows for analyze the request after execution (for example, perform execution time
     * calculation, API Response caching and most important response result to
     * {@link com.ideal.restless.Request.SuccessCallback}) and {@link com.ideal.restless.Request.ErrorCallback}
     *
     * @param requestContext The request Context
     * @param request        The current API request
     * @param threadPool     The thread pool which responsible for the request.
     * @param t              The exception that thrown during the execution of the request
     */
    void postExecute(Map requestContext, Request request, RequestThreadPool threadPool, Throwable t);

    class SkipExecuteException extends RuntimeException {

    }

}

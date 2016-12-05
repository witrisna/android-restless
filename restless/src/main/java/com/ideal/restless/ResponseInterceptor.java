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
 * Handle the response of the request. The {@link ResponseHandler} chain set by
 * {@link com.ideal.restless.Request.Builder#setResponseHandlers(ResponseHandler...)} will be
 * executed in sequence until one of the
 * {@link ResponseHandler#onResponse(Request, RequestThreadPool, Throwable)} return true or
 * end of the chain.
 */
class ResponseInterceptor implements RequestInterceptor {


    @Override
    public void preExecute(Map requestContext, RequestThreadPool threadPool, Request request) {

    }

    @Override
    public void postExecute(Map requestContext, Request request, RequestThreadPool threadPool, Throwable t) {
        //Do not response when request is canceled or threadpool is purged.
        RunnableRequest r = (RunnableRequest) request;
        if (!r.shouldDiscard()) {
            for (ResponseHandler policy : request.getResponseHandler()) {
                if (policy.onResponse(request, threadPool, t)) {
                    break;
                }
            }
        }
    }

}

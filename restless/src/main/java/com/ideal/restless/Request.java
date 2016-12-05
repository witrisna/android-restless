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
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

import static com.ideal.restless.RequestThreadPool.DEBUG;
import static com.ideal.restless.RequestThreadPool.TAG;

/**
 * This class contains information necessary to request an HTTP API Request.
 */
public interface Request extends Runnable {

    /**
     * Cancel the request. The callback will not be invoked after cancel.
     */
    void cancel();

    /**
     * @return The Uri for the request.
     */
    Uri getUri();

    /**
     * @return Number of retry attempted.
     */
    int getRetryAttempted();

    /**
     * @return Maximum of retry
     */
    int getMaxRetry();

    /**
     * @return The retry interval
     */
    int getRetryInterval();

    /**
     * @return The headers of the request
     */
    Map<String, List<String>> getHeaders();

    /**
     * @return The post/put body content
     */
    Object getBody();

    /**
     * @return True when this request can be cached.
     */
    boolean isCachable();

    /**
     * @return The http method for the request.
     */
    String getMethod();

    ResponseHandler[] getResponseHandler();

    ResponseHandler[] DEFAULT_POLICY = {new RetryPolicy(), new ResponseCallbackPolicy()};

    interface Method {
        String GET = "GET";
        String POST = "POST";
        String PUT = "PUT";
        String DELETE = "DELETE";
    }

    /**
     * Callback interface for delivering success responses.
     */
    interface SuccessCallback<T> {

        /**
         * Callback method that success response is received.
         *
         * @param body         The transformed result of the response body.
         * @param httpResponse The HTTP Response
         */
        void onSuccess(T body, HttpResponse httpResponse);
    }

    /**
     * Callback interface for delivering error responses.
     */
    interface ErrorCallback {

        /**
         * Callback method that an error has been occurred
         *
         * @param t            The Exception that cause the error.
         * @param httpResponse The HTTP Response
         */
        void onError(Throwable t, HttpResponse httpResponse);
    }

    class Builder<T> {
        private static final int LIMIT = 10485760; //10MB
        private Uri uri;
        private String method = Method.GET;
        private Map<String, List<String>> headers = new HashMap<>();
        private Object body;
        private boolean cacheable = false;
        private boolean responseOnBackgroundThread = false;
        private SuccessCallback<T> successCallback;
        private ErrorCallback errorCallback;
        private int retry;
        private int retryInterval = 500; //In Millisecond
        private int timeout = 3000; //Millisecond
        private ResponseHandler[] responseHandlers = DEFAULT_POLICY;
        private DataMarshaller<T> responseMarshaller;

        private byte[] slurpStream(InputStream stream) throws IOException {
            final byte[] buf = new byte[4096];
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            int got, total = 0;
            while ((got = stream.read(buf)) > 0) {
                out.write(buf, 0, got);
                total += got;
                if (total >= LIMIT)
                    throw new IOException("Stream limit exceeded");
            }
            return out.toByteArray();
        }

        public Builder(Uri uri) {
            this.uri = uri;
        }

        /**
         * Set request to GET method
         */
        public Builder<T> get() {
            this.method = Method.GET;
            return this;
        }

        /**
         * Set request to PUT method
         *
         * @param body
         * @return
         */
        public Builder<T> put(Object body) {
            this.method = Method.PUT;
            this.body = body;
            return this;
        }

        public Builder<T> post(Object body) {
            this.method = Method.POST;
            this.body = body;
            return this;
        }

        public Builder<T> delete(Object body) {
            method = Method.DELETE;
            this.body = body;
            return this;
        }

        /**
         * Enable cache for this request. By default false.
         */
        public Builder<T> cacheable() {
            this.cacheable = true;
            return this;
        }

        /**
         * Response callback on background thread. By default response callback on Main Thread.
         */
        public Builder<T> responseOnBackgroundThread() {
            this.responseOnBackgroundThread = true;
            return this;
        }

        /**
         * Add header to the request.
         */
        public Builder<T> header(String name, String value) {
            List<String> values = headers.get(name);
            if (values == null) {
                values = new ArrayList<>();
                headers.put(name, values);
            }
            values.add(value);
            return this;
        }

        /**
         * Set the number of retry for this request.
         */
        public Builder<T> retry(int retry) {
            this.retry = retry;
            return this;
        }

        /**
         * Set the retry interval, the wait time to make another try.
         * The value is in millisecond and default to 500 ms
         */
        public Builder<T> retryInterval(int retryInterval) {
            this.retryInterval = retryInterval;
            return this;
        }


        /**
         * Provide Success callback, {@link SuccessCallback#onSuccess(Object, HttpResponse)}
         * will be called when a success response is received.
         */
        public Builder<T> onSuccess(SuccessCallback<T> callback) {
            this.successCallback = callback;
            return this;
        }

        /**
         * Provide Error callback, {@link ErrorCallback#onError(Throwable, HttpResponse)}
         * will be called when a error response is received.
         */
        public Builder<T> onError(ErrorCallback callback) {
            this.errorCallback = callback;
            return this;
        }

        /**
         * Provide {@link ResponseHandler} chain set to handle the response.
         * The chain will be executed in sequence until one of the
         * {@link ResponseHandler#onResponse(Request, RequestThreadPool, Throwable)} return true or
         * end of the chain. By default {@link RetryPolicy} and {@link ResponseCallbackPolicy} are
         * defined.
         */
        public Builder<T> setResponseHandlers(ResponseHandler... policies) {
            this.responseHandlers = policies;
            return this;
        }

        /**
         * Provide the {@link DataMarshaller} to transform the response data to Object.
         * The {@link DataMarshaller#unmarshal(byte[])} will be called to transform the data.
         * <p>
         * <p>If {@link DataMarshaller} is not defined, base on the the response content-type,
         * the {@link DataMarshalProvider} will try to transform the data by the registered
         * {@link DataMarshaller}. {@link DataMarshalProvider} uses
         * {@link DataMarshaller#responseContentType()} to map with the response content type to find
         * the {@link DataMarshaller}
         */
        public Builder<T> setResponseMarshaller(DataMarshaller<T> dataMarshaller) {
            this.responseMarshaller = dataMarshaller;
            return this;
        }

        /**
         * Set the timeout of the request. Default to 3000ms
         */
        public Builder<T> timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Request build() {

            return new RunnableRequest<T>() {

                private boolean isCanceled;
                private long attachedTime;
                private Future future;
                private CacheControl cacheControl = new CacheControl.Builder().build(headers);

                @Override
                public void cancel() {
                    if (future != null) {
                        future.cancel(true);
                    }
                    isCanceled = true;
                }

                private HttpResponse response;
                private int retryAttempted;
                private RequestThreadPool threadPool;
                private HttpURLConnection connection;


                @Override
                public void onSuccess() throws Exception {
                    if (responseMarshaller == null) {
                        onSuccess((T) DataMarshalProvider.getInstance().unmarshal(response.getContentType(),
                                response.getBody()), response);
                    } else {
                        onSuccess((T) responseMarshaller.unmarshal(response.getBody()), response);
                    }
                }

                @Override
                public void setResponse(HttpResponse response) {
                    this.response = response;
                }

                @Override
                public Uri getUri() {
                    return uri;
                }

                @Override
                public int getRetryAttempted() {
                    return retryAttempted;
                }

                @Override
                public int getMaxRetry() {
                    return retry;
                }

                @Override
                public int getRetryInterval() {
                    return retryInterval;
                }

                @Override
                public Map<String, List<String>> getHeaders() {
                    return headers;
                }

                @Override
                public Object getBody() {
                    return body;
                }

                @Override
                public void incrementRetryCount() {
                    ++retryAttempted;
                }

                @Override
                public boolean isCachable() {
                    if (cacheable) {
                        if (cacheControl != null) {
                            return !cacheControl.isNoCache() && !cacheControl.isNoStore();
                        }
                        return true;
                    }
                    return false;
                }

                @Override
                public String getMethod() {
                    return method;
                }

                @Override
                public ResponseHandler[] getResponseHandler() {
                    return responseHandlers;
                }

                @Override
                public void onAttached(RequestThreadPool threadPool) {
                    this.threadPool = threadPool;
                    this.attachedTime = System.currentTimeMillis();
                }

                @Override
                public void onAttached(Future future) {
                    this.future = future;
                }

                @Override
                public boolean shouldDiscard() {

                    if (isCanceled) {
                        return true;
                    } else {
                        if (attachedTime > 0 && threadPool != null) {
                            return (attachedTime < threadPool.getLastPurgeTime());
                        } else {
                            return false;
                        }
                    }
                }

                void onSuccess(final T result, final HttpResponse response) {

                    if (!shouldDiscard()) {
                        if (successCallback != null) {
                            if (responseOnBackgroundThread) {
                                successCallback.onSuccess(result, response);
                            } else {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        successCallback.onSuccess(result, response);
                                    }
                                });
                            }
                        }
                    }
                }

                public void onError(final Throwable t) {
                    if (!shouldDiscard()) {
                        if (errorCallback != null) {
                            if (responseOnBackgroundThread) {
                                errorCallback.onError(t, response);
                            } else {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        errorCallback.onError(t, response);
                                    }
                                });
                            }
                        }
                    }
                }

                @Override
                public HttpResponse getResponse() {
                    return response;
                }

                @Override
                public HttpURLConnection getConnection() {
                    return connection;
                }

                @Override
                public CacheControl getCacheControl() {
                    return cacheControl;
                }

                private void preExecute(Map requestContext) {
                    RequestInterceptor[] interceptors = threadPool.getInterceptors();
                    if (interceptors != null) {
                        for (RequestInterceptor interceptor : threadPool.getInterceptors()) {
                            Log.d(TAG, String.format("Request %s, preExecute by %s",
                                    uri, interceptor.getClass().getName()));
                            interceptor.preExecute(requestContext, threadPool, this);
                        }
                    }
                }

                private void postExecute(Map requestContext, Throwable t) {
                    RequestInterceptor[] interceptors = threadPool.getInterceptors();
                    if (interceptors != null) {
                        for (int i = interceptors.length - 1; i >= 0; i--) {
                            Log.d(TAG, String.format("Request %s, postExecute by %s",
                                    uri, interceptors[i].getClass().getName()));
                            interceptors[i].postExecute(requestContext, this, threadPool, t);
                        }
                    }
                }

                private void execute(HttpURLConnection conn) {
                    try {
                        conn.setConnectTimeout(timeout);
                        conn.setReadTimeout(timeout);
                        conn.setRequestMethod(method);

                        if ("https".equals(uri.getScheme()) && threadPool.getSSLSocketFactory() != null) {
                            ((HttpsURLConnection) conn).setSSLSocketFactory(threadPool.getSSLSocketFactory());
                        }

                        for (String key : headers.keySet()) {
                            if (headers.get(key) != null) {
                                for (String value : headers.get(key)) {
                                    conn.setRequestProperty(key, value);
                                }
                            }
                        }

                        if (body != null) {
                            Pair<String, byte[]> result = DataMarshalProvider.getInstance().marshal(body);
                            byte[] bodyBytes = result.second;
                            conn.setDoOutput(true);

                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                                if (bodyBytes.length > 0) {
                                    conn.setFixedLengthStreamingMode(bodyBytes.length);
                                } else {
                                    conn.setChunkedStreamingMode(0);
                                }
                            }
                            if (result.first != null) {
                                conn.setRequestProperty("Content-Type", result.first);
                            }
                            conn.getOutputStream().write(bodyBytes);
                            conn.getOutputStream().close();
                        }

                        int responseStatusCode = conn.getResponseCode();
                        Map<String, List<String>> responseHeaders = conn.getHeaderFields();
                        String contentType = conn.getContentType();

                        InputStream inputStream = null;
                        try {
                            inputStream = conn.getInputStream();
                            byte[] data = slurpStream(inputStream);
                            response = new HttpResponse(responseStatusCode, contentType, responseHeaders, data);
                        } catch (IOException ioe) {
                            inputStream = conn.getErrorStream();
                            byte[] data = slurpStream(inputStream);
                            response = new HttpResponse(responseStatusCode, contentType, responseHeaders, data);
                            throw ioe;
                        }
                        inputStream.close();
                    } catch (Exception t) {
                        throw new RequestFailedException(t);
                    }
                }

                @Override
                public void run() {

                    if (DEBUG) Log.d(TAG, String.format("Request %s is running", uri));

                    try {
                        connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
                    } catch (IOException e) {
                        errorCallback.onError(e, null);
                        if (DEBUG) Log.d(TAG, String.format("OpenConnection to %s failed", uri), e);
                        return;
                    }

                    //Template method for preExecute, execute and postExecute
                    //If preExecute throw exception skip the execute
                    Map requestContext = new HashMap();
                    Throwable thrown = null;
                    try {
                        preExecute(requestContext);
                        execute(connection);
                    } catch (RequestInterceptor.SkipExecuteException e) {
                        if (DEBUG) Log.d(TAG, String.format("Request %s skipped", uri));
                        //do nothing
                    } catch (Throwable x) {
                        if (DEBUG) Log.d(TAG, String.format("Request %s error", uri));
                        thrown = x;
                    } finally {
                        try {
                            postExecute(requestContext, thrown);
                        } catch (Throwable t) {
                            if (DEBUG) Log.d(TAG,
                                    String.format("Request %s postExecute failed", uri), t);
                            //Ignore
                        }
                    }
                }
            };
        }
    }


    class RequestFailedException extends RuntimeException {

        RequestFailedException(Throwable cause) {
            super(cause);
        }
    }
}

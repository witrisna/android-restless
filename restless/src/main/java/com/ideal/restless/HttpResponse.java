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

import java.util.List;
import java.util.Map;

/**
 * After receiving and interpreting a request message, a server responds
 * with an HTTP response message.
 */
public class HttpResponse {

    private Integer statusCode;
    private String contentType;
    private Map<String, List<String>> responseHeaders;
    private byte[] body;

    public HttpResponse(Integer statusCode, String contentType, Map<String, List<String>> responseHeaders, byte[] body) {
        this.statusCode = statusCode;
        this.contentType = contentType;
        this.responseHeaders = responseHeaders;
        this.body = body;
    }

    /**
     * @return The response content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @return The Status-Code element is a 3-digit integer result code of the attempt to
     * understand and satisfy the request.
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * @return The response headers
     */
    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * @return The body content
     */
    public byte[] getBody() {
        return body;
    }
}

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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;

/**
 * Responsible to transforming an Object to a data format suitable for Http outbound (execute to Http Server),
 * and inbound (receive data from Http Server)
 *
 * @param <T> The data type of the Object
 */
public interface DataMarshaller<T> {

    /**
     * Transform a {@link byte[]} to an Object
     *
     * @param content The raw data as byte[]
     * @return The Object represent with the provided content
     * @throws Exception Error occur during the transformation
     */
    T unmarshal(byte[] content) throws Exception;

    /**
     * Transform a {@link T} to {@link byte[]}
     *
     * @param data The object to be transformed
     * @return The raw data as byte[]
     * @throws Exception Error occur during the transformation
     */
    byte[] marshal(T data) throws Exception;

    /**
     * @return The Class type of the object
     */
    Class<T> getType();

    /**
     * @return The content type for outbound (Send to Http Server)
     */
    String requestContentType();

    /**
     * @return The content types for inbound
     */
    String[] responseContentType();

    /**
     * Transform a {@link LinkedHashMap} to a URL encoded Form
     * <em>Note:</em> Inbound is not supported for this Data Type
     */
    class UrlEncodedFormMarshaller implements DataMarshaller<LinkedHashMap> {

        @Override
        public LinkedHashMap unmarshal(byte[] content) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] marshal(LinkedHashMap data) throws Exception {
            StringBuilder sb = new StringBuilder();
            for (Object key : data.keySet()) {
                String name;
                try {
                    name = URLEncoder.encode(key.toString(), "UTF-8");
                    String value = data.get(key) == null ? null : URLEncoder.encode(data.get(key.toString()).toString(), "UTF-8");
                    if (sb.length() > 0) {
                        sb.append("&");
                    }
                    sb.append(name);
                    if (value != null) {
                        sb.append("=");
                        sb.append(value);
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
            return sb.toString().getBytes("ISO_8859_1");
        }

        @Override
        public Class<LinkedHashMap> getType() {
            return LinkedHashMap.class;
        }

        @Override
        public String requestContentType() {
            return "application/x-www-form-urlencoded";
        }

        @Override
        public String[] responseContentType() {
            return null;
        }
    }

    class JsonDataMarshaller implements DataMarshaller<JSONObject> {

        @Override
        public JSONObject unmarshal(byte[] content) throws UnsupportedEncodingException, JSONException {
            return new JSONObject(new String(content, "UTF-8"));
        }

        @Override
        public byte[] marshal(JSONObject data) throws UnsupportedEncodingException {
            return data.toString().getBytes("UTF-8");
        }

        @Override
        public Class<JSONObject> getType() {
            return JSONObject.class;
        }

        @Override
        public String requestContentType() {
            return "application/json";
        }

        @Override
        public String[] responseContentType() {
            return new String[]{"application/json"};
        }
    }

    class JsonArrayDataMarshaller implements DataMarshaller<JSONArray> {

        @Override
        public JSONArray unmarshal(byte[] content) throws UnsupportedEncodingException, JSONException {
            return new JSONArray(new String(content, "UTF-8"));
        }

        @Override
        public byte[] marshal(JSONArray data) throws UnsupportedEncodingException {
            return data.toString().getBytes("UTF-8");
        }

        @Override
        public Class<JSONArray> getType() {
            return JSONArray.class;
        }

        @Override
        public String requestContentType() {
            return "application/json";
        }

        @Override
        public String[] responseContentType() {
            return new String[]{"application/json"};
        }
    }


    class StringMarshaller implements DataMarshaller<String> {

        @Override
        public String unmarshal(byte[] content) throws UnsupportedEncodingException {
            return new String(content, "UTF-8");
        }

        @Override
        public byte[] marshal(String data) throws UnsupportedEncodingException {
            return data.getBytes("UTF-8");
        }

        @Override
        public Class<String> getType() {
            return String.class;
        }

        @Override
        public String requestContentType() {
            return "text/plain";
        }

        @Override
        public String[] responseContentType() {
            return new String[]{"text/plain", "text/html"};
        }
    }

    class ByteArrayMarshaller implements DataMarshaller<byte[]> {

        @Override
        public byte[] unmarshal(byte[] content) throws Exception {
            return content;
        }

        @Override
        public byte[] marshal(byte[] data) throws Exception {
            return data;
        }

        @Override
        public Class<byte[]> getType() {
            return byte[].class;
        }

        @Override
        public String requestContentType() {
            return "application/octet-stream";
        }

        @Override
        public String[] responseContentType() {
            return new String[]{"application/octet-stream"};
        }
    }

    /**
     * Transform a {@link Bitmap} to/from PNG image format
     * To support gif, jpg, jpeg,
     */
    class BitmapMarshaller implements DataMarshaller<Bitmap> {

        @Override
        public Bitmap unmarshal(byte[] content) throws Exception {
            return BitmapFactory.decodeByteArray(content, 0, content.length);
        }

        @Override
        public byte[] marshal(Bitmap data) throws Exception {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            data.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return stream.toByteArray();
        }

        @Override
        public Class<Bitmap> getType() {
            return Bitmap.class;
        }

        @Override
        public String requestContentType() {
            return "image/png";
        }

        @Override
        public String[] responseContentType() {
            return new String[] {"image/png", "image/jpg", "image/gif"};
        }
    }



}

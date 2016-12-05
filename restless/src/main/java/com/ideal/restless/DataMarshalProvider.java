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

import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@link DataMarshalProvider} provide serialize/deserialize data, it transforms Object to byte[]
 * and from byte[] to Object
 */
public class DataMarshalProvider {

    private static DataMarshalProvider instance = new DataMarshalProvider();

    private Map<Class, DataMarshaller> outboundMarshallers = new HashMap<>();
    private Map<String, List<DataMarshaller>> inboundMarshallers = new HashMap<>();

    private DataMarshalProvider() {
        register(new DataMarshaller.JsonDataMarshaller());
        register(new DataMarshaller.StringMarshaller());
        register(new DataMarshaller.ByteArrayMarshaller());
        register(new DataMarshaller.JsonArrayDataMarshaller());
        register(new DataMarshaller.UrlEncodedFormMarshaller());
    }

    /**
     * Register {@link DataMarshaller}
     */
    public void register(DataMarshaller dataMarshaller) {
        if (dataMarshaller.responseContentType() != null) {
            for (String responseContentType : dataMarshaller.responseContentType()) {
                List<DataMarshaller> list = inboundMarshallers.get(responseContentType);
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(dataMarshaller);
                inboundMarshallers.put(responseContentType, list);
            }
        }
        if (dataMarshaller.requestContentType() != null) {
            outboundMarshallers.put(dataMarshaller.getType(), dataMarshaller);
        }
    }


    public static DataMarshalProvider getInstance() {
        return instance;
    }

    private DataMarshaller findMarshaller(Object obj) {
        return outboundMarshallers.get(obj.getClass());
    }

    private List<DataMarshaller> findMarshaller(String contentType) {
        for (String s : inboundMarshallers.keySet()) {
            if (contentType.contains(s.toLowerCase())) {
                return inboundMarshallers.get(s);
            }
        }
        return null;
    }

    /**
     * Marshall data according to types or conventions on data objects
     *
     * @param object The data to marshal
     * @return {@link Pair#first} for the content type of the object, {@link Pair#second} for the
     * serialized data.
     * @throws Exception Error occur during the transformation
     */
    Pair<String, byte[]> marshal(Object object) throws Exception {
        DataMarshaller marshaller = findMarshaller(object);
        if (marshaller != null) {
            return new Pair<>(marshaller.requestContentType(), marshaller.marshal(object));
        } else {
            throw new UnsupportedOperationException("Unsupport object type");
        }
    }


    /**
     * Unmarshall data according to the content type
     *
     * @param contentType The content type of the data
     * @param data        The raw data as byte[]
     * @return The Object represent with the provided content
     */
    Object unmarshal(String contentType, byte[] data) {
        List<DataMarshaller> marshallers = findMarshaller(contentType);
        if (marshallers != null) {
            for (DataMarshaller m : marshallers) {
                try {
                    return m.unmarshal(data);
                } catch (Exception ignored) {
                }
            }
        }
        return data;
    }


}

package com.ideal.restless;

import android.net.http.HttpResponseCache;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

public class FileCache implements Cache<Request, HttpResponse> {

    private File file;
    private long size;

    public FileCache(File file, long size) throws IOException {
        this.file = file;
        this.size = size;
        HttpResponseCache.install(file, size);
    }

    @Override
    public HttpResponse get(Request key) {
        HttpURLConnection connection = ((RunnableRequest) key).getConnection();
        if (connection != null) {
            connection.setUseCaches(true);
        }
        return null;
    }

    @Override
    public void put(Request key, HttpResponse value) {
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
        }
    }

    @Override
    public void clear() throws IOException {
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.delete();
        }
        HttpResponseCache.install(file, size);
    }
}

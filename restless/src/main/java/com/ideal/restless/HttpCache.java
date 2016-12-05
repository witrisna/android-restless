package com.ideal.restless;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Http caching which implement https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9
 */
public abstract class HttpCache implements Cache<Request, HttpResponse> {

    /**
     * Find the {@link CacheEntry} by request
     */
    abstract CacheEntry find(Request request);

    /**
     * Delete the {@link CacheEntry} by request
     */
    abstract void delete(Request request);

    /**
     * Delete all {@link CacheEntry}
     */
    abstract void deleteAll();

    /**
     * Create a {@link CacheEntry} which associate with the request
     */
    abstract void create(Request request, CacheEntry cacheEntry);


    @Override
    public HttpResponse get(Request request) {

        RunnableRequest rr = (RunnableRequest) request;
        rr.getConnection().setUseCaches(false);
        if (request.getMethod().equals(Request.Method.GET)) {
            CacheEntry cacheEntry = find(request);
            if (cacheEntry != null) {

                if (rr.getCacheControl() != null) {
                    if (rr.getCacheControl().onlyIfCached()) {
                        return cacheEntry.getResponse();
                    }
                }

                if (cacheEntry.getETag() != null) {
                    List<String> values = new ArrayList<>();
                    values.add(cacheEntry.getETag());
                    request.getHeaders().put("If-None-Match", values);
                }

                CacheControl cc = cacheEntry.getCacheControl();
                if (cc.isMustRevalidate() ||
                        cc.isProxyRevalidate() ||
                        cc.isNoCache()) {
                    return null;
                } else if (isExpired(cacheEntry)) {
                    if (cacheEntry.getETag() == null) {
                        delete(request);
                    }
                    return null;
                } else {
                    return cacheEntry.getResponse();
                }
            }
        }
        return null;
    }

    private boolean isExpired(CacheEntry cacheEntry) {
        long now = (new Date()).getTime();

        //If a response includes an s-maxage directive, then for a shared cache
        //(but not for a private cache), the maximum age specified by this directive overrides
        // the maximum age specified by either the max-age directive or the Expires header.
        if (cacheEntry.getCacheControl().getSmaxAge() != null) {
            if ((now - cacheEntry.getDate() - (cacheEntry.getCacheControl().getSmaxAge() * 1000)) > 0) {
                return true;
            }
        }

        if (cacheEntry.getCacheControl().getMaxAge() != null) {
            if ((now - cacheEntry.getDate() - (cacheEntry.getCacheControl().getMaxAge() * 1000)) > 0) {
                return true;
            }
        }

        if (cacheEntry.getExpires() != null) {
            if ((now - cacheEntry.getExpires()) > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void put(Request key, HttpResponse value) {
        if (key.getMethod().equals(Request.Method.GET)) {
            if (value.getStatusCode().equals(HttpURLConnection.HTTP_NOT_MODIFIED)) {
                CacheEntry cacheEntry = find(key);
                if (cacheEntry != null) {
                    ((RunnableRequest) key).setResponse(cacheEntry.getResponse());
                    return;
                }
            }
            CacheEntry cacheEntry = new CacheEntry(value);
            if (cacheEntry.getCacheControl() == null
                    || cacheEntry.getCacheControl().isNoStore()) {
                return;
            }
            create(key, cacheEntry);
        }
    }

    @Override
    public void clear() throws IOException {
        deleteAll();
    }



}

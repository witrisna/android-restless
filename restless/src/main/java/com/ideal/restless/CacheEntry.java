package com.ideal.restless;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CacheEntry {

    private HttpResponse response;
    private String eTag;
    private CacheControl cacheControl;
    private long date;
    private Long expires;

    public CacheEntry(HttpResponse response) {
        this.response = response;
        this.eTag = getETag(response);
        this.cacheControl = new CacheControl.Builder().build(response.getResponseHeaders());
        this.date = getDate(response);
        this.expires = getExpires(response);
    }

    public long getDate() {
        return date;
    }

    public Long getExpires() {
        return expires;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public String getETag() {
        return eTag;
    }

    public CacheControl getCacheControl() {
        return cacheControl;
    }


    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    private Long getDate(HttpResponse response) {
        List<String> date = response.getResponseHeaders().get("Date");
        if (date != null && date.size() == 1) {
            try {
                return dateFormat.parse(date.get(0)).getTime();
            } catch (ParseException e) {
                return (new Date()).getTime();
            }
        }
        return (new Date()).getTime();
    }

    private Long getExpires(HttpResponse response) {
        List<String> expires = response.getResponseHeaders().get("Expires");
        if (expires != null && expires.size() == 1) {
            try {
                return dateFormat.parse(expires.get(0)).getTime();
            } catch (ParseException e) {
                return 0L;
            }
        }
        return null;
    }

    private String getETag(HttpResponse response) {
        List<String> etag = response.getResponseHeaders().get("ETag");
        if (etag != null && etag.size() == 1) {
            return etag.get(0);
        }
        return null;
    }
}

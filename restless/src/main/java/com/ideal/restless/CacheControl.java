package com.ideal.restless;

import java.util.List;
import java.util.Map;

/**
 * Represent CacheControl attributed which defined under
 * https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9
 */
public interface CacheControl {


    boolean isNoCache();

    boolean isNoStore();

    boolean isNoTransform();

    boolean isMustRevalidate();

    boolean isProxyRevalidate();

    Long getMaxAge();

    Long getSmaxAge();

    boolean onlyIfCached();

    class Builder {

        boolean noCache;
        boolean noStore;
        boolean noTransform;
        boolean isPrivate;
        boolean isPublic;
        boolean mustRevalidate;
        boolean proxyRevalidate;
        boolean onlyIfCached;
        Long maxAge;
        Long smaxAge;

        Builder noCache() {
            noCache = true;
            return this;
        }

        Builder noStore() {
            noStore = true;
            return this;
        }

        Builder noTransform() {
            noTransform = true;
            return this;
        }

        Builder isPrivate() {
            isPrivate = true;
            return this;
        }

        Builder isPublic() {
            isPublic = true;
            return this;
        }

        Builder mustRevalidate() {
            mustRevalidate = true;
            return this;
        }

        Builder proxyRevalidate() {
            proxyRevalidate = true;
            return this;
        }

        Builder maxAge(Long maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        Builder sMaxAge(Long smaxAge) {
            this.smaxAge = smaxAge;
            return this;
        }

        Builder onlyIfCached() {
            this.onlyIfCached = true;
            return this;
        }

        CacheControl build() {
            return new CacheControl() {
                @Override
                public boolean isNoCache() {
                    return noCache;
                }

                @Override
                public boolean isNoStore() {
                    return noStore;
                }

                @Override
                public boolean isNoTransform() {
                    return noTransform;
                }

                @Override
                public boolean isMustRevalidate() {
                    return mustRevalidate;
                }

                @Override
                public boolean isProxyRevalidate() {
                    return proxyRevalidate;
                }

                @Override
                public Long getMaxAge() {
                    return maxAge;
                }

                @Override
                public Long getSmaxAge() {
                    return smaxAge;
                }

                @Override
                public boolean onlyIfCached() {
                    return onlyIfCached;
                }
            };
        }

        CacheControl build(Map<String, List<String>> headers) {
            if (headers != null) {
                List<String> cacheControl = headers.get("Cache-Control");
                if (cacheControl != null) {
                    if (cacheControl.size() == 1) {
                        String[] directives = cacheControl.get(0).split(",");
                        CacheControl.Builder builder = new CacheControl.Builder();
                        for (String d : directives) {
                            d = d.trim();
                            if (d.startsWith("no-cache")) {
                                builder.noCache();
                                continue;
                            }
                            if (d.startsWith("no-store")) {
                                builder.noStore();
                                continue;
                            }
                            if (d.startsWith("no-transform")) {
                                builder.noTransform();
                                continue;
                            }
                            if (d.startsWith("must-revalidate")) {
                                builder.mustRevalidate();
                                continue;
                            }
                            if (d.startsWith("proxy-revalidate")) {
                                builder.proxyRevalidate();
                                continue;
                            }
                            if (d.startsWith("public")) {
                                builder.isPublic();
                                continue;
                            }
                            if (d.startsWith("private")) {
                                builder.isPrivate();
                                continue;
                            }
                            if (d.startsWith("max-age=")) {
                                builder.maxAge(Long.parseLong(d.substring(8)));
                                continue;
                            }
                            if (d.startsWith("s-maxage=")) {
                                builder.maxAge(Long.parseLong(d.substring(9)));
                                continue;
                            }
                            if (d.startsWith("only-if-cached")) {
                                builder.onlyIfCached();
                                continue;
                            }

                        }
                        return builder.build();
                    }
                }
            }
            return null;
        }

        ;
    }
}

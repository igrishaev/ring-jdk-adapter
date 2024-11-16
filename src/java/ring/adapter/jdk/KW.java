package ring.adapter.jdk;

import clojure.lang.Keyword;

public class KW {
    public static final Keyword body = Keyword.intern("body");
    public static final Keyword uri = Keyword.intern("uri");
    public static final Keyword protocol = Keyword.intern("protocol");
    public static final Keyword remote_addr = Keyword.intern("remote-addr");
    public static final Keyword request_method = Keyword.intern("request-method");
    public static final Keyword scheme = Keyword.intern("scheme");
    public static final Keyword headers = Keyword.intern("headers");
    public static final Keyword server_name = Keyword.intern("server-name");
    public static final Keyword server_port = Keyword.intern("server-port");
    public static final Keyword status = Keyword.intern("status");
    public static final Keyword query_string = Keyword.intern("query-string");
    public static final Keyword get = Keyword.intern("get");
    public static final Keyword post = Keyword.intern("post");
    public static final Keyword put = Keyword.intern("put");
    public static final Keyword delete = Keyword.intern("delete");
    public static final Keyword patch = Keyword.intern("patch");
    public static final Keyword options = Keyword.intern("options");
    public static final Keyword http = Keyword.intern("http");
    @SuppressWarnings("unused")
    public static final Keyword https = Keyword.intern("https");
    @SuppressWarnings("unused")
    public static final Keyword ws = Keyword.intern("ws");
    @SuppressWarnings("unused")
    public static final Keyword wss = Keyword.intern("wss");

    public static String KWtoString(final Keyword kw) {
        return kw.toString().substring(1);
    }
}




package ring.jdk;

import clojure.lang.*;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.net.URI;

public class Handler implements HttpHandler {

    private final IFn ringHandler;

    public Handler(final IFn ringHandler) {
        this.ringHandler = ringHandler;
    }

    private static IPersistentMap toClojureHeaders(final Headers headers) {
        IPersistentMap result = PersistentHashMap.EMPTY;
        for (Map.Entry<String, List<String>> me: headers.entrySet()) {
            if (me.getValue().size() == 1) {
                result = result.assoc(me.getKey(), me.getValue().get(0));
            } else {
                result = result.assoc(me.getKey(), PersistentVector.create(me.getValue()));
            }
        }
        return result;
    }

    private static void sendHeaders(final Map<?,?> ringHeaders, final Headers javaHeaders) {
        Object k, v;
        for (Map.Entry<?,?> me: ringHeaders.entrySet()) {
            k = me.getKey();
            v = me.getValue();
            if (k instanceof String ks) {
                if (v instanceof String vs) {
                    javaHeaders.add(ks, vs);
                } else if (v instanceof Iterable<?> iterable){
                    for (Object vi: iterable) {
                        if (vi instanceof String vis) {
                            javaHeaders.add(ks, vis);
                        } else {
                            throw Err.error("unsupported header value: %s", vi);
                        }
                    }
                }
            } else {
                throw Err.error("header name is not a string: %s", k);
            }
        }
    }

    private Map<?,?> toRequest(final HttpExchange httpExchange) {
        final String protocol = httpExchange.getProtocol();
        final String method = httpExchange.getRequestMethod();
        final URI uri = httpExchange.getRequestURI();
        final InputStream body = httpExchange.getRequestBody();
        final InetSocketAddress remoteAddress = httpExchange.getRemoteAddress();
        final Headers headers = httpExchange.getRequestHeaders();
        final int serverPort = httpExchange.getHttpContext().getServer().getAddress().getPort();

        return PersistentHashMap.create(
                KW.body, body,
                KW.uri, uri.toString(),
                KW.protocol, protocol,
                KW.remote_addr, remoteAddress.toString(),
                KW.request_method, method,
                // KW.scheme, "dunno",
                KW.headers, toClojureHeaders(headers),
                // KW.server_name, "dunno",
                KW.server_port, serverPort
        );
    }

    private void sendStatus(final HttpExchange exchange, final int status, final long len) {
        try {
            exchange.sendResponseHeaders(status, 0);
        } catch (IOException e) {
            throw Err.error(e, "cannot send response headers, status: %s, response length: %s", status, len);
        }
    }

    private void sendResponse(final int status,
                              final Map<?,?> ringHeaders,
                              final Object bodyObj,
                              final HttpExchange exchange) {

        final Headers javaHeaders = exchange.getResponseHeaders();
        final OutputStream out = exchange.getResponseBody();

        if (bodyObj == null) {
            sendStatus(exchange, status, 0);
            sendHeaders(ringHeaders, javaHeaders);
            IO.close(out);

        } else if (bodyObj instanceof InputStream in) {
            sendStatus(exchange, status, 0);
            sendHeaders(ringHeaders, javaHeaders);
            IO.transfer(in, out);
            IO.close(out);

        } else if (bodyObj instanceof File file) {
            sendStatus(exchange, status, file.length());
            sendHeaders(ringHeaders, javaHeaders);
            IO.transfer(IO.toInputStream(file), out);
            IO.close(out);

        } else if (bodyObj instanceof String s) {
            final byte[] ba = s.getBytes(StandardCharsets.UTF_8);
            sendStatus(exchange, status, ba.length);
            sendHeaders(ringHeaders, javaHeaders);
            IO.transfer(new ByteArrayInputStream(ba), out);
            IO.close(out);

        } else if (bodyObj instanceof byte[] ba) {
            sendStatus(exchange, status, ba.length);
            sendHeaders(ringHeaders, javaHeaders);
            IO.transfer(new ByteArrayInputStream(ba), out);
            IO.close(out);

        } else if (bodyObj instanceof Iterable<?> iterable) {
            sendStatus(exchange, status, 0);
            sendHeaders(ringHeaders, javaHeaders);
            for (Object item: iterable) {
                if (item instanceof String s) {
                    IO.write(out, s, StandardCharsets.UTF_8);
                } else {
                    throw Err.error("item %s is not a string:", item);
                }
            }
            IO.close(out);

        } else {
            IO.close(out);
            throw Err.error("ring body is unsupported: %s", bodyObj);
        }
    }

    private int getStatus(final Map<?,?> ringResponse) {
        final Object x = ringResponse.get(KW.status);
        if (x instanceof Integer i) {
            return i;
        } else {
            throw Err.error("ring status code is not integer: %s", x);
        }
    }

    private Map<?,?> getHeaders(final Map<?,?> ringResponse) {
        final Object x = ringResponse.get(KW.headers);
        if (x == null) {
            return PersistentHashMap.EMPTY;
        } else if (x instanceof Map<?,?> m) {
            return m;
        } else {
            throw Err.error("unsupported ring headers: %s", x);
        }
    }

    private void sendResponse(final Object response, final HttpExchange exchange) {
        if (response instanceof Map<?,?> clojureResponse) {
            final int status = getStatus(clojureResponse);
            final Map<?,?> headers = getHeaders(clojureResponse);
            final Object body = clojureResponse.get(KW.body);
            sendResponse(status, headers, body, exchange);
        } else {
            throw Err.error("ring response is not a map: %s", response);
        }
    }

    @Override
    public void handle(HttpExchange exchange) {
        final Map<?,?> request = toRequest(exchange);
        // try catch
        final Object response = ringHandler.invoke(request);
        sendResponse(response, exchange);
    }
}

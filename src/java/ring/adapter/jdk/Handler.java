package ring.adapter.jdk;

import clojure.lang.*;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.InetSocketAddress;
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
        String header;
        for (Map.Entry<String, List<String>> me: headers.entrySet()) {
            // ring relies on lower-cased headers
            header = me.getKey().toLowerCase();
            if (me.getValue().size() == 1) {
                result = result.assoc(header, me.getValue().get(0));
            } else {
                result = result.assoc(header, PersistentVector.create(me.getValue()));
            }
        }
        return result;
    }

    private static Keyword toClojureMethod(final String method) {
        return switch (method) {
            case "GET" -> KW.get;
            case "POST" -> KW.post;
            case "PUT" -> KW.put;
            case "PATCH" -> KW.patch;
            case "DELETE" -> KW.delete;
            case "OPTIONS" -> KW.options;
            default -> Keyword.intern(method.toLowerCase());
        };
    }

    private Map<?,?> toRequest(final HttpExchange httpExchange) {
        final String protocol = httpExchange.getProtocol();
        final String method = httpExchange.getRequestMethod();
        final URI uri = httpExchange.getRequestURI();
        final String queryString = uri.getQuery();
        final InputStream body = httpExchange.getRequestBody();
        final InetSocketAddress remoteAddress = httpExchange.getRemoteAddress();
        final Headers headers = httpExchange.getRequestHeaders();
        final int serverPort = httpExchange.getHttpContext().getServer().getAddress().getPort();
        final String serverName = httpExchange.getHttpContext().getServer().getAddress().getHostName();
        final String uriString = uri.getPath();

        return PersistentHashMap.create(
                KW.body, body,
                KW.uri, uriString,
                KW.protocol, protocol,
                KW.remote_addr, remoteAddress.toString(),
                KW.request_method, toClojureMethod(method),
                KW.query_string, queryString,
                KW.headers, toClojureHeaders(headers),
                KW.server_name, serverName,
                KW.server_port, serverPort,
                // TODO: implement HTTPs/SSL
                KW.scheme, KW.http
        );
    }

    private void sendStatus(final HttpExchange exchange, final int httpStatus, final long contentLength) {
        try {
            exchange.sendResponseHeaders(httpStatus, contentLength);
        } catch (IOException e) {
            throw Err.error(e,
                    "cannot send response headers, status: %s, response length: %s",
                    httpStatus, contentLength
            );
        }
    }

    private void sendResponse(final Response response, final HttpExchange exchange) {
        final Headers headers = exchange.getResponseHeaders();
        for (Header h: response.headers()) {
            headers.add(h.k(), h.v());
        }
        sendStatus(exchange, response.status(), response.contentLength());
        final OutputStream out = exchange.getResponseBody();
        final InputStream bodyStream = response.bodyStream();
        if (bodyStream != null) {
            IO.transfer(bodyStream, out);
        }
        final Iterable<?> bodyIter = response.bodyIter();
        if (bodyIter != null) {
            for (Object x: bodyIter) {
                if (x != null) {
                    IO.transfer(x.toString(), out);
                }
            }
        }
        IO.close(out);
    }

    @Override
    public void handle(HttpExchange exchange) {
        final Map<?,?> request = toRequest(exchange);
        Object ringResponse;
        Response javaResponse;
        try {
            ringResponse = ringHandler.invoke(request);
            javaResponse = Response.fromRingResponse(ringResponse);
        } catch (Exception e) {
            javaResponse = Response.get500response(e, "failed to execute ring handler");
        }
        sendResponse(javaResponse, exchange);
    }
}

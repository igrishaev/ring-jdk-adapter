package ring.jdk;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import clojure.lang.PersistentArrayMap;
import clojure.lang.PersistentHashMap;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.net.URI;

public class RingHandler implements HttpHandler {

    private final IFn ringHandler;

    public RingHandler(final IFn ringHandler) {
        this.ringHandler = ringHandler;
    }

    private Map<?,?> toRequest(final HttpExchange httpExchange) {
        final String schema = httpExchange.getProtocol();
        final String method = httpExchange.getRequestMethod();
        final URI uri = httpExchange.getRequestURI();
        final InputStream body = httpExchange.getRequestBody();

        return PersistentHashMap.create(

        );
    }

    private void sendResponse(final Object response, final HttpExchange exchange) {

    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        final Map<?,?> request = toRequest(exchange);
        final Object response = ringHandler.invoke(request);
        sendResponse(response, exchange);
    }



}

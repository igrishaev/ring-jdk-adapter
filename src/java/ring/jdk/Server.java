package ring.jdk;

import clojure.lang.IFn;
import com.sun.net.httpserver.HttpServer;

public class Server implements AutoCloseable {

    private final HttpServer httpServer;
    private final String host;
    private final int port;
    private final IFn clojureHandler;

    private Server(final String host, final int port, final IFn clojureHandler) {
        this.host = host;
        this.port = port;
        this.clojureHandler = clojureHandler;
    }

    private void start() {
        httpServer = HttpServer.create();
        httpServer.bind();
        httpServer.a
    }

    @Override
    public void close() throws Exception {
        httpServer.stop(1000);
    }
}

package ring.jdk;

import clojure.lang.IFn;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Server implements AutoCloseable {

    private HttpServer httpServer;
    private final HttpHandler javaHandler;
    private final InetSocketAddress address;

    public static int STOP_DELAY_MS = 1000;

    private Server(final String host, final int port, final IFn clojureHandler) {
        this.address = new InetSocketAddress(host, port);
        this.javaHandler = new RingHandler(clojureHandler);
    }

    @SuppressWarnings("unused")
    public static Server start(final String host, final int port, final IFn clojureHandler) {
        return new Server(host, port, clojureHandler).init();
    }

    private Server init() {
        try {
            httpServer = HttpServer.create(address, 0);
        } catch (IOException e) {
            throw Err.error(e, "failed to create HTTP server, addr: %s", address);
        }
        httpServer.createContext("/", javaHandler);
        httpServer.setExecutor(null);
        httpServer.start();
        return this;
    }

    @Override
    public void close() {
        httpServer.stop(STOP_DELAY_MS);
    }
}

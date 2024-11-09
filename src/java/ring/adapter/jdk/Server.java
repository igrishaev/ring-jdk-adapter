package ring.adapter.jdk;

import clojure.lang.IFn;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Server implements AutoCloseable {

    private final Config config;
    private final IFn ringHandler;
    private HttpServer httpServer;

    private Server(final IFn ringHandler, final Config config) {
        this.ringHandler = ringHandler;
        this.config = config;
    }

    @Override
    public String toString() {
        return String.format("<Server %s:%s, handler: %s>",
                config.host(),
                config.port(),
                ringHandler.toString()
        );
    }

    @SuppressWarnings("unused")
    public static Server start(final IFn clojureHandler) {
        return start(clojureHandler, Config.DEFAULT);
    }

    @SuppressWarnings({"unused", "resource"})
    public static Server start(final IFn ringHandler, final Config config) {
        return new Server(ringHandler, config).init();
    }

    private Server init() {
        final InetSocketAddress address = new InetSocketAddress(
                config.host(), config.port()
        );
        try {
            httpServer = HttpServer.create(address, config.socket_backlog());
        } catch (IOException e) {
            throw Err.error(e, "failed to create HTTP server, addr: %s", address);
        }
        final Handler javaHandler = new Handler(ringHandler);
        httpServer.createContext(config.root_path(), javaHandler);
        final int threads = config.threads();
        Executor executor;
        if (threads > 0) {
            executor = Executors.newFixedThreadPool(threads);
        } else {
            executor = config.executor();
        }
        httpServer.setExecutor(executor);
        httpServer.start();
        return this;
    }

    @Override
    public void close() {
        httpServer.stop(config.stop_delay_sec());
    }
}

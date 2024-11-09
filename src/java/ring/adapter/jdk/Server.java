package ring.adapter.jdk;

import clojure.lang.IFn;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Server implements AutoCloseable {

    private HttpServer httpServer;
    private final Config config;

    private Server(final Config config) {
        this.config = config;
    }

    @SuppressWarnings({"unused", "resource"})
    public static Server start(final IFn clojureHandler, final int port) {
        final Config config = Config.builder(clojureHandler).port(port).build();
        return new Server(config).init();
    }

    @SuppressWarnings({"unused", "resource"})
    public static Server start(final Config config) {
        return new Server(config).init();
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
        final Handler javaHandler = new Handler(config.ringHandler());
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

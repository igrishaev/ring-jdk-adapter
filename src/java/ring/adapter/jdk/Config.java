package ring.adapter.jdk;

import clojure.lang.IFn;

import java.util.concurrent.Executor;

public record Config(
        IFn ringHandler,
        String host,
        int port,
        int stop_delay_sec,
        String root_path,
        int threads,
        Executor executor,
        int socket_backlog
) {

    public static Builder builder(final IFn ringHandler) {
        return new Builder(ringHandler);
    }

    public static class Builder {

        public Builder(final IFn ringHandler) {
            this.ringHandler = ringHandler;
        }

        private final IFn ringHandler;
        private String host = Const.host;
        private int port = Const.port;
        private int stop_delay_sec = Const.stop_delay_sec;
        private String root_path = Const.root_path;
        private int threads = Const.threads;
        private Executor executor = Const.executor;
        private int socket_backlog = Const.socket_backlog;

        @SuppressWarnings("unused")
        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder stop_delay_sec(final int stop_delay_sec) {
            this.stop_delay_sec = stop_delay_sec;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder root_path(final String root_path) {
            this.root_path = root_path;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder threads(final int threads) {
            this.threads = threads;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder executor(final Executor executor) {
            this.executor = executor;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder socket_backlog(final int socket_backlog) {
            this.socket_backlog = socket_backlog;
            return this;
        }

        public Config build() {
            return new Config(
                    ringHandler,
                    host,
                    port,
                    stop_delay_sec,
                    root_path,
                    threads,
                    executor,
                    socket_backlog
            );
        }
    }
}

package ring.adapter.jdk;

import java.util.concurrent.Executor;

public class Const {
    public static String host = "127.0.0.1";
    public static int port = 8080;
    public static int stop_delay_sec = 0;
    public static String root_path = "/";
    public static int threads = 0;
    public static Executor executor = null;
    public static int socket_backlog = 0;
}

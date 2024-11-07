package ring.jdk;

import clojure.lang.AFn;
import clojure.lang.PersistentHashMap;

public class Main {

    public static void main(String... args) {
        final Server s = Server.start("127.0.0.1", 8080, new AFn() {
            @Override
            public Object invoke(Object request) {
                return PersistentHashMap.create(
                        KW.status, 200,
                        KW.headers, PersistentHashMap.create("content-type", "text/plain"),
                        KW.body, "hello"
                );
            }
        });
        System.out.println("test");
    }
}

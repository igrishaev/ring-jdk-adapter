package ring.adapter.jdk;

import clojure.lang.AFn;
import clojure.lang.PersistentHashMap;

public class Main {

    public static void main(String... args) {
        final Server s = Server.start(new AFn() {
            @Override
            public Object invoke(Object request) {
                int a = 0 / 1;
                return PersistentHashMap.create(
                        KW.status, 200,
                        KW.headers, PersistentHashMap.create("content-type", "text/plain"),
                        KW.body, 42
                );
            }
        });
        System.out.println("test");
    }
}

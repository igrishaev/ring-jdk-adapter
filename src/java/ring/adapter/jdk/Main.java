package ring.adapter.jdk;

import clojure.java.api.Clojure;
import clojure.lang.AFn;
import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentList;
import clojure.lang.PersistentVector;

public class Main {

    public static void main(String... args) {
        final Server s = Server.start(new AFn() {
            @Override
            public Object invoke(Object request) {
                int a = 0 / 1;
                return PersistentHashMap.create(
                        KW.status, 200,
                        KW.headers, PersistentHashMap.create("content-type", "text/plain"),
                        KW.body, Clojure.var("clojure.core", "map").invoke(Clojure.var("clojure.core", "/"), PersistentVector.create(3, 2, 1, 0, 1), PersistentVector.create(3, 2, 1, 0, 1))
                );
            }
        });
        System.out.println("test");
    }
}

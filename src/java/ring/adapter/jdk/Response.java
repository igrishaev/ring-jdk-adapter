package ring.adapter.jdk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import clojure.lang.RT;

public record Response (
        int status,
        List<Header> headers,
        InputStream bodyStream,
        Iterable<?> bodyIter,
        long contentLength
) {

    public static Response get500response(final Throwable e, final String message) {
        final List<Header> headers = List.of(new Header("Content-Type", "text/plain"));
        final StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        final String payload = message + "\n" + sw.toString();
        final byte[] buf = payload.getBytes(StandardCharsets.UTF_8);
        return new Response(
                500,
                headers,
                new ByteArrayInputStream(buf),
                null,
                buf.length
        );
    }

    public static int getStatus(final Map<?,?> ringResponse) {
        final Object x = ringResponse.get(KW.status);
        if (x instanceof Number n) {
            return RT.intCast(n);
        } else {
            throw Err.error("ring status is not integer: %s", x);
        }
    }

    public static List<Header> getHeaders(final Map<?,?> ringResponse) {
        final Object x = ringResponse.get(KW.headers);

        if (x == null) {
            return List.of();
        }

        if (x instanceof Map<?,?> m) {
            Object k, v;
            final List<Header> result = new ArrayList<>(m.size());
            for (Map.Entry<?,?> me: m.entrySet()) {
                k = me.getKey();
                v = me.getValue();
                if (k instanceof String ks) {
                    if (v instanceof String vs) {
                        result.add(new Header(ks, vs));
                    } else if (v instanceof Iterable<?> iterable) {
                        for (Object vi : iterable) {
                            if (vi instanceof String vis) {
                                result.add(new Header(ks, vis));
                            } else {
                                throw Err.error("unsupported header value: %s", vi);
                            }
                        }
                    }
                } else {
                    throw Err.error("header name is not a string: %s", k);
                }
            }
            return result;
        }
        throw Err.error("wrong ring headers: %s", x);
    }

    public static Response fromRingResponse(final Object x) {
        if (x instanceof Map<?,?> ringResponse) {
            final int status = getStatus(ringResponse);
            final List<Header> headers = getHeaders(ringResponse);

            final Object bodyObj = ringResponse.get(KW.body);
            InputStream bodyStream = null;
            Iterable<?> bodyIter = null;
            long contentLength = 0;

            if (bodyObj instanceof InputStream in) {
                bodyStream = in;
            } else if (bodyObj instanceof File file) {
                bodyStream = IO.toInputStream(file);
                contentLength = file.length();
            } else if (bodyObj instanceof String s) {
                final byte[] buf = s.getBytes(StandardCharsets.UTF_8);
                bodyStream = new ByteArrayInputStream(buf);
                contentLength = buf.length;
            } else if (bodyObj == null) {
                bodyStream = InputStream.nullInputStream();
            } else if (bodyObj instanceof Iterable<?> i) {
                bodyIter = i;
            } else {
                throw Err.error("unsupported ring body: %s", bodyObj);
            }

            return new Response(status, headers, bodyStream, bodyIter, contentLength);

        } else {
            throw Err.error("unsupported ring response: %s", x);
        }

    }

}
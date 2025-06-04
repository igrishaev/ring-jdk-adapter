package ring.adapter.jdk;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class IO {

    public static void transfer(final InputStream in, final OutputStream out) {
        try {
            in.transferTo(out);
        } catch (IOException e) {
            throw Err.error("could not transfer ab input stream into the output stream");
        }
    }

    public static void transfer(final String s, final OutputStream out) {
        final byte[] buf = s.getBytes(StandardCharsets.UTF_8);
        try {
            out.write(buf);
            out.flush();
        } catch (IOException e) {
            throw Err.error("could not transfer a string into the output stream");
        }
    }

    public static InputStream toInputStream(final File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw Err.error(e, "file not found: %s", file);
        }
    }

    @SuppressWarnings("unused")
    public static void write(final OutputStream out, final String s) {
        write(out, s, StandardCharsets.UTF_8);
    }

    public static void write(final OutputStream out, final String s, final Charset charset) {
        try {
            out.write(s.getBytes(charset));
        } catch (IOException e) {
            throw Err.error(e, "could not write string to the output stream");
        }
    }

    public static void close(final OutputStream out) {
        try {
            out.close();
        } catch (IOException e) {
            throw Err.error(e, "cannot close the output stream");
        }
    }

}

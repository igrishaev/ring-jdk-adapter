package ring.jdk;

public class Err {

    private Err(){}

    public static RuntimeException error(final Throwable e, final String template, final Object... args) {
        return new RuntimeException(String.format(template, args), e);
    }

    public static RuntimeException error(final String template, final Object... args) {
        return new RuntimeException(String.format(template, args));
    }

}

package bndtools.classpath;

public class BndContainerException extends Exception {

    private static final long serialVersionUID = 1L;

    public BndContainerException() {
        super();
    }

    public BndContainerException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public BndContainerException(String message) {
        super(message);
    }

    public BndContainerException(Throwable throwable) {
        super(throwable);
    }
}

package hk.edu.polyu.comp.vlabcontroller;

public class VLabControllerException extends RuntimeException {

    private static final long serialVersionUID = 5221979016901962537L;

    public VLabControllerException(String message) {
        super(message);
    }

    public VLabControllerException(String message, Throwable cause) {
        super(message, cause);
    }

}

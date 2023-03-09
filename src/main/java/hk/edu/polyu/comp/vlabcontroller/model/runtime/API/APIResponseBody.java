package hk.edu.polyu.comp.vlabcontroller.model.runtime.API;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;

public class APIResponseBody<T> implements Serializable {
    @Getter
    private final int code;
    @Getter
    private final T data;
    @Getter
    private final String message;
    @Getter
    private final long timestamp;

    private APIResponseBody(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ResponseEntity<APIResponseBody<T>> success () {
        APIResponseBody<T> apiResponseBody = new APIResponseBody<>(HttpStatus.OK.value(), null, "success");
        return new ResponseEntity<>(apiResponseBody, HttpStatus.OK);
    }

    public static <T> ResponseEntity<APIResponseBody<T>> success (T data) {
        APIResponseBody<T> apiResponseBody = new APIResponseBody<>(HttpStatus.OK.value(), data, "success");
        return new ResponseEntity<>(apiResponseBody, HttpStatus.OK);
    }

    public static <T> ResponseEntity<APIResponseBody<T>> failed () {
        APIResponseBody<T> apiResponseBody = new APIResponseBody<>(HttpStatus.FORBIDDEN.value(), null, "failed");
        return new ResponseEntity<>(apiResponseBody, HttpStatus.FORBIDDEN);
    }

    public static <T> ResponseEntity<APIResponseBody<T>> failed (String message) {
        APIResponseBody<T> apiResponseBody = new APIResponseBody<>(HttpStatus.FORBIDDEN.value(), null, message);
        return new ResponseEntity<>(apiResponseBody, HttpStatus.FORBIDDEN);
    }

    public static <T> ResponseEntity<APIResponseBody<T>> failed (T data) {
        APIResponseBody<T> apiResponseBody = new APIResponseBody<>(HttpStatus.FORBIDDEN.value(), data, "failed");
        return new ResponseEntity<>(apiResponseBody, HttpStatus.FORBIDDEN);
    }

    public static <T> ResponseEntity<APIResponseBody<T>> failed (String message, T data) {
        APIResponseBody<T> apiResponseBody = new APIResponseBody<>(HttpStatus.FORBIDDEN.value(), data, message);
        return new ResponseEntity<>(apiResponseBody, HttpStatus.FORBIDDEN);
    }

    public static <T> ResponseEntity<APIResponseBody<T>> badRequest () {
        APIResponseBody<T> apiResponseBody = new APIResponseBody<>(HttpStatus.BAD_REQUEST.value(), null, "bad request");
        return new ResponseEntity<>(apiResponseBody, HttpStatus.BAD_REQUEST);
    }

    public static <T> ResponseEntity<APIResponseBody<T>> badRequest (String message) {
        APIResponseBody<T> apiResponseBody = new APIResponseBody<>(HttpStatus.BAD_REQUEST.value(), null, message);
        return new ResponseEntity<>(apiResponseBody, HttpStatus.BAD_REQUEST);
    }

    public static <T> ResponseEntity<APIResponseBody<T>> badRequest (T data) {
        APIResponseBody<T> apiResponseBody = new APIResponseBody<>(HttpStatus.BAD_REQUEST.value(), data, "bad request");
        return new ResponseEntity<>(apiResponseBody, HttpStatus.BAD_REQUEST);
    }

    public static <T> ResponseEntity<APIResponseBody<T>> badRequest (String message, T data) {
        APIResponseBody<T> apiResponseBody = new APIResponseBody<>(HttpStatus.BAD_REQUEST.value(), data, message);
        return new ResponseEntity<>(apiResponseBody, HttpStatus.BAD_REQUEST);
    }

    public static <T> ResponseEntity<APIResponseBody<T>> resourceNotFound () {
        APIResponseBody<T> apiResponseBody = new APIResponseBody<>(HttpStatus.NOT_FOUND.value(), null, "resource not found");
        return new ResponseEntity<>(apiResponseBody, HttpStatus.NOT_FOUND);
    }

    public static <T> ResponseEntity<APIResponseBody<T>> notFound (String message) {
        APIResponseBody<T> apiResponseBody = new APIResponseBody<>(HttpStatus.NOT_FOUND.value(), null, message);
        return new ResponseEntity<>(apiResponseBody, HttpStatus.NOT_FOUND);
    }

}

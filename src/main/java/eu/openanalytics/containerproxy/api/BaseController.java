package eu.openanalytics.containerproxy.api;

import lombok.experimental.StandardException;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.inject.Inject;

public class BaseController {

    @Inject
    private Environment environment;

    protected void prepareMap(ModelMap map) {
        map.put("title", environment.getProperty("proxy.title", "ShinyProxy"));
    }

    @StandardException
    public static class NotFoundException extends RuntimeException {
        private static final long serialVersionUID = 2042632906716154791L;
    }

    @ControllerAdvice
    public static class RestErrorHandler {
        @ExceptionHandler(NotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        @ResponseBody
        public Object notFound(NotFoundException ex) {
            return ex.getMessage();
        }
    }
}

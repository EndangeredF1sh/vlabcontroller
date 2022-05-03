package hk.edu.polyu.comp.vlabcontroller.api;

import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import lombok.Setter;
import lombok.experimental.StandardException;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.inject.Inject;

@RefreshScope
public class BaseController {
    @Setter(onMethod_ = {@Inject})
    protected ProxyProperties proxyProperties;

    protected void prepareMap(ModelMap map) {
        map.put("title", proxyProperties.getTitle());
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

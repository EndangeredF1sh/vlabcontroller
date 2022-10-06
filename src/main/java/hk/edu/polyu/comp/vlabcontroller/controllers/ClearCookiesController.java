package hk.edu.polyu.comp.vlabcontroller.controllers;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
@Controller
public class ClearCookiesController {
    @GetMapping(value = "/clearcookies", produces = MediaType.APPLICATION_JSON_VALUE)
    public String clearCookies(ModelMap map, HttpServletRequest request, HttpServletResponse response) {
        var cookies = request.getCookies();
        Arrays.stream(cookies).forEach(cookie -> {
            cookie.setValue("");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        });
        return "clear-cookies";
    }
}

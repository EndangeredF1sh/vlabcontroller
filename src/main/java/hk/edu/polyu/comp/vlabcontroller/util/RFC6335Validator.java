package hk.edu.polyu.comp.vlabcontroller.util;

public class RFC6335Validator {
    public static boolean valid(String input) {
        return input.matches("^(?!.*--.*)[^\\W_]([^\\W_]|-)*(?<!-)$");
    }
}

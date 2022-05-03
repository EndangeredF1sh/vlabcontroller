package hk.edu.polyu.comp.vlabcontroller.util;

import io.vavr.Function1;

import java.time.Duration;

public class DurationUtil {
    public static Duration max(Duration a, Duration b) {
        return a.compareTo(b) > 0 ? a : b;
    }
    public static Duration min(Duration a, Duration b) {
        return a.compareTo(b) < 0 ? a : b;
    }
    public static Function1<Duration, Duration> atLeast(Duration least) {
        return x -> min(x, least).equals(x) ? least : x;
    }
    public static Function1<Duration, Duration> atMost(Duration most) {
        return x -> max(x, most).equals(x) ? most : x;
    }
}

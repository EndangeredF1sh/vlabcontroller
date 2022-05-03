package hk.edu.polyu.comp.vlabcontroller.util;

import java.time.Duration;

/**
 * A listener that keeps track of whether a channel is active.
 */
public class ChannelActiveListener implements Runnable {

    private Duration lastWrite = Duration.ZERO;

    @Override
    public void run() {
        lastWrite = Duration.ofMillis(System.currentTimeMillis());
    }

    /**
     * Checks whether the channel was active in the provided period.
     */
    public boolean isActive(Duration period) {
        var diff = Duration.ofMillis(System.currentTimeMillis()).minus(lastWrite);

        // make sure the period is at least 5 seconds
        // this ensures that when the socket is active, the ping is delayed for at least 5 seconds
        return diff.compareTo(DurationUtil.atLeast(Duration.ofSeconds(5)).apply(period)) <= 0;
    }

}

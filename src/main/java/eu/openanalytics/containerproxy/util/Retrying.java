package eu.openanalytics.containerproxy.util;

import java.util.function.IntPredicate;

public class Retrying {

    public static boolean retry(IntPredicate job, int tries, int waitTime) {
        return retry(job, tries, waitTime, false);
    }

    public static boolean retry(IntPredicate job, int tries, int waitTime, boolean retryOnException) {
        boolean retVal = false;
        RuntimeException exception = null;
        for (int currentTry = 1; currentTry <= tries; currentTry++) {
            try {
                if (job.test(currentTry)) {
                    retVal = true;
                    exception = null;
                    break;
                }
            } catch (RuntimeException e) {
                if (retryOnException) exception = e;
                else throw e;
            }
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException ignore) {
            }
        }
        if (exception == null) return retVal;
        else throw exception;

    }
}

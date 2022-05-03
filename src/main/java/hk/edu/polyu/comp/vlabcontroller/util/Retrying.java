package hk.edu.polyu.comp.vlabcontroller.util;

import io.vavr.control.Try;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

@Component
public class Retrying {
    @Async
    public CompletableFuture<Boolean> retry(IntPredicate job, int tries, Duration waitTime) {
        return retry(job, tries, waitTime, false);
    }

    @Async
    public CompletableFuture<Boolean> retry(IntPredicate job, int tries, Duration waitTime, boolean retryOnException) {
        var result = Try.success(false);
        for (var currentTry : (Iterable<Integer>) () -> IntStream.rangeClosed(1, tries).iterator()) {
            result = Try.of(() -> job.test(currentTry))
                .recoverWith(e -> retryOnException ? Try.success(false) : Try.failure(e));
            if (result.isFailure()) return CompletableFuture.failedFuture(result.getCause());
            if (result.get()) return CompletableFuture.completedFuture(true);
            try {
                Thread.sleep(waitTime.toMillis());
            } catch (InterruptedException ignored) {
            }
        }
        if (result.isFailure()) return CompletableFuture.failedFuture(result.getCause());
        return CompletableFuture.completedFuture(result.get());
    }
}

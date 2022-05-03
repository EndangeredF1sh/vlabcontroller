package hk.edu.polyu.comp.vlabcontroller.model.runtime;

import lombok.*;

import java.time.Duration;

@Data @Builder(toBuilder = true) @AllArgsConstructor
public class HeartbeatStatus {
    private Duration startRecordTimestamp;
    private Duration lastRecordTimestamp;
    private int totalPayloadLength;
    private int terminateCounter;

    public HeartbeatStatus() {
        this.startRecordTimestamp = Duration.ofMillis(System.currentTimeMillis());
        this.lastRecordTimestamp = this.startRecordTimestamp;
    }

    public void increaseCounter() {
        terminateCounter++;
    }

    public void clearAll() {
        startRecordTimestamp = Duration.ofMillis(System.currentTimeMillis());
        totalPayloadLength = 0;
        terminateCounter = 0;
    }
}

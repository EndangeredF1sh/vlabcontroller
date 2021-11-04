package eu.openanalytics.containerproxy.model.runtime;

import lombok.Getter;
import lombok.Setter;

public class HeartbeatStatus {
  @Getter private long startRecordTimestamp;
  @Getter @Setter private long lastRecordTimestamp;
  @Getter @Setter private int totalPayloadLength;
  @Getter private int terminateCounter;
  
  public HeartbeatStatus() {
    this.startRecordTimestamp = System.currentTimeMillis();
    this.lastRecordTimestamp = this.startRecordTimestamp;
  }
  
  public void increaseCounter() {
    terminateCounter++;
  }
  
  public void clearAll() {
    startRecordTimestamp = System.currentTimeMillis();
    totalPayloadLength = 0;
    terminateCounter = 0;
  }
}

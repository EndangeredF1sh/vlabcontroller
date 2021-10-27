package eu.openanalytics.containerproxy.model.runtime;

public class HeartbeatStatus {
    private long startRecordTimestamp;
    private long lastRecordTimestamp;
    private int totalPayloadLength;
    private int terminateCounter;

    public HeartbeatStatus () {
        this.startRecordTimestamp = System.currentTimeMillis();
        this.lastRecordTimestamp = this.startRecordTimestamp;
    }

    public long getStartRecordTimestamp() {
        return startRecordTimestamp;
    }

    public long getLastRecordTimestamp() {
        return lastRecordTimestamp;
    }

    public void setLastRecordTimestamp(long lastRecordTimestamp) {
        this.lastRecordTimestamp = lastRecordTimestamp;
    }

    public int getTotalPayloadLength() {
        return totalPayloadLength;
    }

    public void setTotalPayloadLength(int totalPayloadLength) {
        this.totalPayloadLength = totalPayloadLength;
    }

    public void increaseCounter(){
        terminateCounter++;
    }

    public int getTerminateCounter(){
        return terminateCounter;
    }

    public void clearAll(){
        startRecordTimestamp = System.currentTimeMillis();
        totalPayloadLength = 0;
        terminateCounter = 0;
    }
}

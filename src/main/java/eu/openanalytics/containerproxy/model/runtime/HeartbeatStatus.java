/**
 * ContainerProxy
 *
 * Copyright (C) 2016-2021 Open Analytics
 *
 * ===========================================================================
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License as published by
 * The Apache Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License for more details.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/>
 */
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

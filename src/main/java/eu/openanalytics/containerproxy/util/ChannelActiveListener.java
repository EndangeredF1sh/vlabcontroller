package eu.openanalytics.containerproxy.util;

/**
 * A listener that keeps track of whether a channel is active.
 */
public class ChannelActiveListener implements Runnable {
  
  private long lastWrite = 0;
  
  @Override
  public void run() {
    lastWrite = System.currentTimeMillis();
  }
  
  /**
   * Checks whether the channel was active in the provided period.
   */
  public boolean isActive(long period) {
    long diff = System.currentTimeMillis() - lastWrite;
    
    // make sure the period is at least 5 seconds
    // this ensures that when the socket is active, the ping is delayed for at least 5 seconds
    if (period < 5000) {
      period = 5000;
    }
  
    return diff <= period;
  }
  
}

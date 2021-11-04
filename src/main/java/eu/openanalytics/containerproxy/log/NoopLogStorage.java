package eu.openanalytics.containerproxy.log;

import eu.openanalytics.containerproxy.model.runtime.Proxy;

import java.io.OutputStream;

public class NoopLogStorage extends AbstractLogStorage {
  
  @Override
  public void initialize() {
    // Do nothing.
  }
  
  @Override
  public OutputStream[] createOutputStreams(Proxy proxy) {
    return null;
  }
  
  @Override
  public String[] getLogs(Proxy proxy) {
    return null;
  }
  
}

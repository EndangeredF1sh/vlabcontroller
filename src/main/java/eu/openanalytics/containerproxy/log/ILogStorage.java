package eu.openanalytics.containerproxy.log;

import eu.openanalytics.containerproxy.model.runtime.Proxy;

import java.io.IOException;
import java.io.OutputStream;

public interface ILogStorage {

    void initialize() throws IOException;

    String getStorageLocation();

    OutputStream[] createOutputStreams(Proxy proxy) throws IOException;

    String[] getLogs(Proxy proxy) throws IOException;

}

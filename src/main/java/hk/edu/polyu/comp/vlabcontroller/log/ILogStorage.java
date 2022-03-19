package hk.edu.polyu.comp.vlabcontroller.log;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;

import java.io.IOException;
import java.io.OutputStream;

public interface ILogStorage {

    void initialize() throws IOException;

    String getStorageLocation();

    OutputStream[] createOutputStreams(Proxy proxy) throws IOException;

    String[] getLogs(Proxy proxy) throws IOException;

}

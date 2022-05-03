package hk.edu.polyu.comp.vlabcontroller.log;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static io.vavr.API.unchecked;

public class FileLogStorage extends AbstractLogStorage {

    @Override
    public void initialize() throws IOException {
        super.initialize();
        Files.createDirectories(Paths.get(containerLogPath));
    }

    @Override
    public OutputStream[] createOutputStreams(Proxy proxy) throws IOException {
        return Arrays.stream(getLogs(proxy)).map(unchecked(x -> new FileOutputStream(x))).toArray(OutputStream[]::new);
    }

}

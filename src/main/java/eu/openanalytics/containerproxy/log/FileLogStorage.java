package eu.openanalytics.containerproxy.log;

import eu.openanalytics.containerproxy.model.runtime.Proxy;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static com.pivovarit.function.ThrowingFunction.unchecked;

public class FileLogStorage extends AbstractLogStorage {
  
  @Override
  public void initialize() throws IOException {
    super.initialize();
    Files.createDirectories(Paths.get(containerLogPath));
  }
  
  @Override
  public OutputStream[] createOutputStreams(Proxy proxy) throws IOException {
    return Arrays.stream(getLogs(proxy)).map(unchecked(FileOutputStream::new)).toArray(OutputStream[]::new);
  }
  
}

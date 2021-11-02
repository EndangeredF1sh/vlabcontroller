package eu.openanalytics.containerproxy.backend.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.LogsParam;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import eu.openanalytics.containerproxy.ContainerProxyException;
import eu.openanalytics.containerproxy.backend.AbstractContainerBackend;
import eu.openanalytics.containerproxy.model.runtime.Container;
import eu.openanalytics.containerproxy.model.runtime.Proxy;
import eu.openanalytics.containerproxy.util.PortAllocator;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.function.BiConsumer;


public abstract class AbstractDockerBackend extends AbstractContainerBackend {
  
  protected static final String PROPERTY_APP_PORT = "port";
  protected static final String PROPERTY_PORT_RANGE_START = "port-range-start";
  protected static final String PROPERTY_PORT_RANGE_MAX = "port-range-max";
  protected static final String DEFAULT_TARGET_URL = DEFAULT_TARGET_PROTOCOL + "://localhost";
  private static final String PROPERTY_PREFIX = "proxy.docker.";
  protected PortAllocator portAllocator;
  protected DockerClient dockerClient;
  
  @Override
  public void initialize() throws ContainerProxyException {
    super.initialize();
    
    int startPort = Integer.parseInt(getProperty(PROPERTY_PORT_RANGE_START, "20000"));
    int maxPort = Integer.parseInt(getProperty(PROPERTY_PORT_RANGE_MAX, "-1"));
    portAllocator = new PortAllocator(startPort, maxPort);
    
    DefaultDockerClient.Builder builder;
    try {
      builder = DefaultDockerClient.fromEnv();
    } catch (DockerCertificateException e) {
      throw new ContainerProxyException("Failed to initialize docker client", e);
    }
    
    String confCertPath = getProperty(PROPERTY_CERT_PATH);
    if (confCertPath != null) {
      try {
        builder.dockerCertificates(DockerCertificates.builder().dockerCertPath(Paths.get(confCertPath)).build().orNull());
      } catch (DockerCertificateException e) {
        throw new ContainerProxyException("Failed to initialize docker client using certificates from " + confCertPath, e);
      }
    }
    
    String confUrl = getProperty(PROPERTY_URL);
    if (confUrl != null) builder.uri(confUrl);
    
    dockerClient = builder.build();
  }
  
  @Override
  public BiConsumer<OutputStream, OutputStream> getOutputAttacher(Proxy proxy) {
    Container c = getPrimaryContainer(proxy);
    if (c == null) return null;
    
    return (stdOut, stdErr) -> {
      try {
        LogStream logStream = dockerClient.logs(c.getId(), LogsParam.follow(), LogsParam.stdout(), LogsParam.stderr());
        logStream.attach(stdOut, stdErr);
      } catch (IOException | InterruptedException | DockerException e) {
        log.error("Error while attaching to container output", e);
      }
    };
  }
  
  @Override
  protected String getPropertyPrefix() {
    return PROPERTY_PREFIX;
  }
  
  protected Container getPrimaryContainer(Proxy proxy) {
    return proxy.getContainers().isEmpty() ? null : proxy.getContainers().get(0);
  }
  
}

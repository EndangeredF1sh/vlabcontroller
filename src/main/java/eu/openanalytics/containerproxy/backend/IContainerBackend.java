package eu.openanalytics.containerproxy.backend;

import eu.openanalytics.containerproxy.ContainerProxyException;
import eu.openanalytics.containerproxy.model.runtime.Proxy;
import eu.openanalytics.containerproxy.model.runtime.ProxyStatus;

import java.io.OutputStream;
import java.util.function.BiConsumer;

public interface IContainerBackend {
  
  /**
   * Initialize this container backend.
   * This method is called lazily, when the backend is needed for the first time.
   *
   * @throws ContainerProxyException If anything goes wrong during initialization of the backend.
   */
  void initialize() throws ContainerProxyException;
  
  /**
   * Start the given proxy, which may take some time depending on the type of backend.
   * The proxy will be in the {@link ProxyStatus#New} state before entering this method.
   * When this method returns, the proxy should be in the {@link ProxyStatus#Up} state.
   *
   * @param proxy The proxy to start up.
   * @throws ContainerProxyException If the startup fails for any reason.
   */
  void startProxy(Proxy proxy) throws ContainerProxyException;
  
  /**
   * Stop the given proxy. Any resources used by the proxy should be released.
   *
   * @param proxy The proxy to stop.
   * @throws ContainerProxyException If an error occurs while stopping the proxy.
   */
  void stopProxy(Proxy proxy) throws ContainerProxyException;
  
  /**
   * Get a function that will forward the standard output and standard error of
   * the given proxy's containers to two output streams.
   * <p>
   * The function will be executed in a separate thread, and is assumed to block
   * until the container stops.
   *
   * @param proxy The proxy whose container output should be attached to the output streams.
   * @return A function that will attach the output, or null if this backend does
   * not support output attaching.
   */
  BiConsumer<OutputStream, OutputStream> getOutputAttacher(Proxy proxy);
}

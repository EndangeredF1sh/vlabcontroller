package eu.openanalytics.containerproxy.util;

import org.xnio.XnioIoThread;
import org.xnio.XnioWorker;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.conduits.ReadReadyHandler;
import org.xnio.conduits.StreamSourceConduit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DelegatingStreamSourceConduit implements StreamSourceConduit {
  
  private final StreamSourceConduit delegate;
  private final Consumer<byte[]> readListener;
  
  public DelegatingStreamSourceConduit(StreamSourceConduit delegate, Consumer<byte[]> readListener) {
    this.delegate = delegate;
    this.readListener = readListener;
  }
  
  @Override
  public void terminateReads() throws IOException {
    delegate.terminateReads();
  }
  
  @Override
  public boolean isReadShutdown() {
    return delegate.isReadShutdown();
  }
  
  @Override
  public void resumeReads() {
    delegate.resumeReads();
  }
  
  @Override
  public void suspendReads() {
    delegate.suspendReads();
  }
  
  @Override
  public void wakeupReads() {
    delegate.wakeupReads();
  }
  
  @Override
  public boolean isReadResumed() {
    return delegate.isReadResumed();
  }
  
  @Override
  public void awaitReadable() throws IOException {
    delegate.awaitReadable();
  }
  
  @Override
  public void awaitReadable(long time, TimeUnit timeUnit) throws IOException {
    delegate.awaitReadable(time, timeUnit);
  }
  
  @Override
  public XnioIoThread getReadThread() {
    return delegate.getReadThread();
  }
  
  @Override
  public void setReadReadyHandler(ReadReadyHandler handler) {
    delegate.setReadReadyHandler(handler);
  }
  
  @Override
  public XnioWorker getWorker() {
    return delegate.getWorker();
  }
  
  @Override
  public long transferTo(long position, long count, FileChannel target) throws IOException {
    return delegate.transferTo(position, count, target);
  }
  
  @Override
  public long transferTo(long count, ByteBuffer throughBuffer, StreamSinkChannel target) throws IOException {
    return delegate.transferTo(count, throughBuffer, target);
  }
  
  @Override
  public int read(ByteBuffer dst) throws IOException {
    if (readListener == null) {
      return delegate.read(dst);
    } else {
      int read = delegate.read(dst);
      ByteBuffer copy = dst.duplicate();
      copy.flip();
      byte[] data = new byte[copy.remaining()];
      copy.get(data);
      readListener.accept(data);
      return read;
    }
  }
  
  @Override
  public long read(ByteBuffer[] dsts, int offs, int len) throws IOException {
    return delegate.read(dsts, offs, len);
  }
  
  
}

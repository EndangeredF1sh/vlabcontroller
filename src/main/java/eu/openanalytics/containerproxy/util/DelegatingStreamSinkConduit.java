package eu.openanalytics.containerproxy.util;

import org.xnio.XnioIoThread;
import org.xnio.XnioWorker;
import org.xnio.channels.StreamSourceChannel;
import org.xnio.conduits.StreamSinkConduit;
import org.xnio.conduits.WriteReadyHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;

public class DelegatingStreamSinkConduit implements StreamSinkConduit {

    private final StreamSinkConduit delegate;
    private final Runnable writeListener;


    public DelegatingStreamSinkConduit(StreamSinkConduit delegate, Runnable writeListener) {
        this.delegate = delegate;
        this.writeListener = writeListener;
    }

    @Override
    public void terminateWrites() throws IOException {
        delegate.terminateWrites();
    }

    @Override
    public boolean isWriteShutdown() {
        return delegate.isWriteShutdown();
    }

    @Override
    public void resumeWrites() {
        delegate.resumeWrites();
    }

    @Override
    public void suspendWrites() {
        delegate.suspendWrites();
    }

    @Override
    public void wakeupWrites() {
        delegate.wakeupWrites();
    }

    @Override
    public boolean isWriteResumed() {
        return delegate.isWriteResumed();
    }

    @Override
    public void awaitWritable() throws IOException {
        delegate.awaitWritable();
    }

    @Override
    public void awaitWritable(long time, TimeUnit timeUnit) throws IOException {
        delegate.awaitWritable(time, timeUnit);
    }

    @Override
    public XnioIoThread getWriteThread() {
        return delegate.getWriteThread();
    }

    @Override
    public void setWriteReadyHandler(WriteReadyHandler handler) {
        delegate.setWriteReadyHandler(handler);
    }

    @Override
    public void truncateWrites() throws IOException {
        delegate.truncateWrites();
    }

    @Override
    public boolean flush() throws IOException {
        return delegate.flush();
    }

    @Override
    public XnioWorker getWorker() {
        return delegate.getWorker();
    }

    @Override
    public long transferFrom(FileChannel src, long position, long count) throws IOException {
        return delegate.transferFrom(src, position, count);
    }

    @Override
    public long transferFrom(StreamSourceChannel source, long count, ByteBuffer throughBuffer) throws IOException {
        return delegate.transferFrom(source, count, throughBuffer);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (writeListener != null) {
            writeListener.run();
        }
        return delegate.write(src);
    }

    public int writeWithoutNotifying(ByteBuffer src) throws IOException {
        return delegate.write(src);
    }

    @Override
    public long write(ByteBuffer[] srcs, int offs, int len) throws IOException {
        return delegate.write(srcs, offs, len);
    }

    @Override
    public int writeFinal(ByteBuffer src) throws IOException {
        return delegate.writeFinal(src);
    }

    @Override
    public long writeFinal(ByteBuffer[] srcs, int offset, int length) throws IOException {
        return delegate.writeFinal(srcs, offset, length);
    }

}

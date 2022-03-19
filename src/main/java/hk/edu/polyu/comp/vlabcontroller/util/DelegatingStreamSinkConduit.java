package hk.edu.polyu.comp.vlabcontroller.util;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.xnio.conduits.StreamSinkConduit;

import java.io.IOException;
import java.nio.ByteBuffer;

@RequiredArgsConstructor
public class DelegatingStreamSinkConduit implements StreamSinkConduit {
    @SuppressWarnings("unused")
    private interface Write {
        int write(ByteBuffer src) throws IOException;
    }

    @Delegate(excludes=Write.class)
    private final StreamSinkConduit delegate;
    private final Runnable writeListener;

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
}

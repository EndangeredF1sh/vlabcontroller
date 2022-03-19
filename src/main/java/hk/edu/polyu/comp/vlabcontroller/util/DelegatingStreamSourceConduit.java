package hk.edu.polyu.comp.vlabcontroller.util;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.xnio.conduits.StreamSourceConduit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class DelegatingStreamSourceConduit implements StreamSourceConduit {
    @SuppressWarnings("unused")
    private interface Read {
        int read(ByteBuffer dst) throws IOException;
    }

    @Delegate(excludes=Read.class)
    private final StreamSourceConduit delegate;
    private final Consumer<byte[]> readListener;

    @Override
    public int read(ByteBuffer dst) throws IOException {
        var read = delegate.read(dst);
        if (readListener != null) {
            var copy = dst.duplicate();
            copy.flip();
            var data = new byte[copy.remaining()];
            copy.get(data);
            readListener.accept(data);
        }
        return read;
    }
}

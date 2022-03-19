package hk.edu.polyu.comp.vlabcontroller.log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.Arrays;

import java.io.*;

//TODO Optimize flushing behaviour
@Slf4j
public class S3LogStorage extends AbstractLogStorage {
    private AmazonS3 s3;
    private TransferManager transferMgr;
    private String bucketName;
    private String bucketPath;
    private boolean enableSSE;

    @Override
    public void initialize() throws IOException {
        super.initialize();

        var accessKey = proxyProperties.getContainerLogS3AccessKey();
        var accessSecret = proxyProperties.getContainerLogS3AccessSecret();
        var endpoint = proxyProperties.getContainerLogS3Endpoint();
        enableSSE = proxyProperties.isContainerLogS3SSE();

        var subPath = containerLogPath.substring("s3://".length()).trim();
        if (subPath.endsWith("/")) subPath = subPath.substring(0, subPath.length() - 1);

        var bucketPathIndex = subPath.indexOf("/");
        if (bucketPathIndex == -1) {
            bucketName = subPath;
            bucketPath = "";
        } else {
            bucketName = subPath.substring(0, bucketPathIndex);
            bucketPath = subPath.substring(bucketPathIndex + 1) + "/";
        }

        s3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new EndpointConfiguration(endpoint, null))
                .enablePathStyleAccess()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, accessSecret)))
                .build();
        transferMgr = TransferManagerBuilder.standard()
                .withS3Client(s3)
                .build();
    }

    @Override
    public OutputStream[] createOutputStreams(Proxy proxy) throws IOException {
        var paths = getLogs(proxy);
        var streams = new OutputStream[2];
        for (var i = 0; i < streams.length; i++) {
            var fileName = paths[i].substring(paths[i].lastIndexOf("/") + 1);
            // TODO kubernetes never flushes. So perform timed flushes, and also flush upon container shutdown
            streams[i] = new BufferedOutputStream(new S3OutputStream(bucketPath + fileName), 1024 * 1024);
        }
        return streams;
    }

    private void doUpload(String key, byte[] bytes) throws IOException {
        var bytesToUpload = bytes;

        var originalBytes = getContent(key);
        if (originalBytes != null) {
            bytesToUpload = Arrays.copyOf(originalBytes, originalBytes.length + bytes.length);
            System.arraycopy(bytes, 0, bytesToUpload, originalBytes.length, bytes.length);
        }

        var metadata = new ObjectMetadata();
        metadata.setContentLength(bytesToUpload.length);
        if (enableSSE) metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);

        if (log.isDebugEnabled())
            log.debug(String.format("Writing log file to S3 [size: %d] [path: %s]", bytesToUpload.length, key));

        InputStream bufferedInput = new BufferedInputStream(new ByteArrayInputStream(bytesToUpload), 20 * 1024 * 1024);
        try {
            transferMgr.upload(bucketName, key, bufferedInput, metadata).waitForCompletion();
        } catch (AmazonClientException | InterruptedException e) {
            throw new IOException(e);
        }
    }

    private byte[] getContent(String key) throws IOException {
        if (s3.doesObjectExist(bucketName, key)) {
            var o = s3.getObject(bucketName, key);
            var out = new ByteArrayOutputStream();
            try (InputStream in = o.getObjectContent()) {
                var buffer = new byte[40 * 1024];
                var len = 0;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }
            return out.toByteArray();
        } else {
            return null;
        }
    }

    private class S3OutputStream extends OutputStream {

        private final String s3Key;

        public S3OutputStream(String s3Key) {
            this.s3Key = s3Key;
        }

        @Override
        public void write(int b) throws IOException {
            // Warning: highly inefficient. Always write arrays.
            var bytesToCopy = new byte[]{(byte) b};
            write(bytesToCopy, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            var bytesToCopy = new byte[len];
            System.arraycopy(b, off, bytesToCopy, 0, len);
            doUpload(s3Key, bytesToCopy);
        }
    }
}

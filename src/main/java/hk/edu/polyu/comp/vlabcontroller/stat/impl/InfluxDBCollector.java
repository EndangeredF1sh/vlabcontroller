package hk.edu.polyu.comp.vlabcontroller.stat.impl;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;


/**
 * E.g.:
 * usage-stats-url: http://localhost:8086/write?db=usagestats
 */
public class InfluxDBCollector extends AbstractDbCollector {
    public String getDestination() {
        return proxyProperties.getUsageStats().getUrl().getInflux();
    }

    @Override
    protected void writeToDb(long timestamp, String userId, String type, String specId, String info) throws IOException {
        var identifier = proxyProperties.getIdentifierValue();
        var body = String.format("event,username=%s,type=%s,identifier=%s specid=\"%s\",info=\"%s\"",
                userId.replace(" ", "\\ "),
                type.replace(" ", "\\ "),
                identifier.replace(" ", "\\ "),
                Optional.ofNullable(specId).orElse(""),
                Optional.ofNullable(info).orElse(""));

        var conn = (HttpURLConnection) new URL(getDestination()).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (var dos = new DataOutputStream(conn.getOutputStream())) {
            dos.write(body.getBytes(StandardCharsets.UTF_8));
            dos.flush();
        }
        var responseCode = conn.getResponseCode();
        if (responseCode == 204) {
            // All is well.
        } else {
            var bos = new ByteArrayOutputStream();
            conn.getErrorStream().transferTo(bos);
            throw new IOException(bos.toString());
        }
    }
}

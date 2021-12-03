package hk.edu.polyu.comp.vlabcontroller.stat.impl;

import org.apache.commons.io.IOUtils;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;


/**
 * E.g.:
 * usage-stats-url: http://localhost:8086/write?db=shinyproxy_usagestats
 */
public class InfluxDBCollector extends AbstractDbCollector {

    private String destination;
    @Inject
    private Environment environment;

    @PostConstruct
    public void init() {
        destination = environment.getProperty("proxy.usage-stats-url.influx-url");
    }

    @Override
    protected void writeToDb(long timestamp, String userId, String type, String specId, String info) throws IOException {
        String identifier = environment.getProperty("proxy.identifier-value", "default-identifier");
        String body = String.format("event,username=%s,type=%s,identifier=%s specid=\"%s\",info=\"%s\"",
                userId.replace(" ", "\\ "),
                type.replace(" ", "\\ "),
                identifier.replace(" ", "\\ "),
                Optional.ofNullable(specId).orElse(""),
                Optional.ofNullable(info).orElse(""));

        HttpURLConnection conn = (HttpURLConnection) new URL(destination).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
            dos.write(body.getBytes(StandardCharsets.UTF_8));
            dos.flush();
        }
        int responseCode = conn.getResponseCode();
        if (responseCode == 204) {
            // All is well.
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copy(conn.getErrorStream(), bos);
            throw new IOException(bos.toString());
        }
    }
}

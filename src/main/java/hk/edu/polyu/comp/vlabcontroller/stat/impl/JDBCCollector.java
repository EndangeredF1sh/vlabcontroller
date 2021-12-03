package hk.edu.polyu.comp.vlabcontroller.stat.impl;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * # MonetDB, Postgresql, MySQL/MariaDB usage-stats-url:
 * jdbc:monetdb://localhost:50000/usage_stats usage-stats-url:
 * jdbc:postgresql://localhost/postgres usage-stats-url:
 * jdbc:mysql://localhost/shinyproxy
 * <p>
 * Assumed table layout:
 * <p>
 * create table event( event_time timestamp, username varchar(128), type
 * varchar(128), data text );
 * <p>
 * <p>
 * # MS SQL Server usage-stats-url:
 * jdbc:sqlserver://localhost;databaseName=shinyproxy
 * <p>
 * Assumed table layout:
 * <p>
 * create table event( event_time datetime, username varchar(128), type
 * varchar(128), data text );
 */
public class JDBCCollector extends AbstractDbCollector {

    private HikariDataSource ds;

    @Inject
    private Environment environment;

    @PostConstruct
    public void init() {
        String baseURL = environment.getProperty("proxy.usage-stats-url.jdbc-url");
        String username = environment.getProperty("proxy.usage-stats-username", "monetdb");
        String password = environment.getProperty("proxy.usage-stats-password", "monetdb");
        ds = new HikariDataSource();
        ds.setJdbcUrl(baseURL);
        ds.setUsername(username);
        ds.setPassword(password);

        Long connectionTimeout = environment.getProperty("proxy.usage-stats-hikari.connection-timeout", Long.class);
        if (connectionTimeout != null) {
            ds.setConnectionTimeout(connectionTimeout);
        }

        Long idleTimeout = environment.getProperty("proxy.usage-stats-hikari.idle-timeout", Long.class);
        if (idleTimeout != null) {
            ds.setIdleTimeout(idleTimeout);
        }

        Long maxLifetime = environment.getProperty("proxy.usage-stats-hikari.max-lifetime", Long.class);
        if (maxLifetime != null) {
            ds.setMaxLifetime(maxLifetime);
        }

        Integer minimumIdle = environment.getProperty("proxy.usage-stats-hikari.minimum-idle", Integer.class);
        if (minimumIdle != null) {
            ds.setMinimumIdle(minimumIdle);
        }

        Integer maximumPoolSize = environment.getProperty("proxy.usage-stats-hikari.maximum-pool-size", Integer.class);
        if (maximumPoolSize != null) {
            ds.setMaximumPoolSize(maximumPoolSize);
        }

    }

    @Override
    protected void writeToDb(long timestamp, String userId, String type, String specId, String info) throws IOException {
        String identifier = environment.getProperty("proxy.identifier-value", "default-identifier");
        String sql = "INSERT INTO event(event_time, username, type, specid, identifier, info) VALUES (?,?,?,?,?,?)";
        try (Connection con = ds.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setTimestamp(1, new Timestamp(timestamp));
                stmt.setString(2, userId);
                stmt.setString(3, type);
                stmt.setString(4, specId);
                stmt.setString(5, identifier);
                stmt.setString(6, info);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IOException("Exception while logging stats", e);
        }
    }
}

package hk.edu.polyu.comp.vlabcontroller.stat.impl;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.*;

/**
 * # MonetDB, Postgresql, MySQL/MariaDB usage-stats-url:
 * jdbc:monetdb://localhost:50000/usage_stats usage-stats-url:
 * jdbc:postgresql://localhost/postgres usage-stats-url:
 * jdbc:mysql://localhost/usage-stats
 * <p>
 * Assumed table layout:
 * <p>
 * create table event( event_time timestamp, username varchar(128), type
 * varchar(128), data text );
 * <p>
 * <p>
 * # MS SQL Server usage-stats-url:
 * jdbc:sqlserver://localhost;databaseName=usage-stats
 * <p>
 * Assumed table layout:
 * <p>
 * create table event( event_time datetime, username varchar(128), type
 * varchar(128), data text );
 */
public class JDBCCollector extends AbstractDbCollector {

    private final Logger log = LogManager.getLogger(getClass());

    private HikariDataSource ds;

    @Inject
    private Environment environment;

    @PostConstruct
    public void init() throws IOException {
        String baseURL = environment.getProperty("proxy.usage-stats-url.jdbc-url");
        String username = environment.getProperty("proxy.usage-stats-username", "monetdb");
        String password = environment.getProperty("proxy.usage-stats-password", "monetdb");
        ds = new HikariDataSource();
        ds.setJdbcUrl(baseURL);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.addDataSourceProperty("useJDBCCompliantTimezoneShift", "true");
        ds.addDataSourceProperty("serverTimezone", "UTC");

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

        try (Connection con = ds.getConnection()) {
            Statement statement = con.createStatement();
            statement.executeUpdate("CREATE TABLE event(event_time TIMESTAMP, username VARCHAR(128), type VARCHAR(128), specid VARCHAR(128), identifier VARCHAR(128), template_name VARCHAR(128), info TEXT)");
        } catch (SQLSyntaxErrorException syntaxErrorException) {
            log.info("Skipping create event table, code: {}, message: {}", syntaxErrorException.getErrorCode(), syntaxErrorException.getMessage());
        } catch (SQLException e) {
            throw new IOException("Exception while initializing table", e);
        }
    }

    @Override
    protected void writeToDb(long timestamp, String userId, String type, String specId, String templateName, String info) throws IOException {
        String identifier = environment.getProperty("proxy.identifier-value", "default-identifier");
        String sql = "INSERT INTO event(event_time, username, type, specid, identifier, template_name, info) VALUES (?,?,?,?,?,?,?)";
        try (Connection con = ds.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setTimestamp(1, new Timestamp(timestamp));
                stmt.setString(2, userId);
                stmt.setString(3, type);
                stmt.setString(4, specId);
                stmt.setString(5, identifier);
                stmt.setString(6, templateName);
                stmt.setString(7, info);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IOException("Exception while logging stats", e);
        }
    }
}

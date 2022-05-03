package hk.edu.polyu.comp.vlabcontroller.stat.impl;

import com.zaxxer.hikari.HikariDataSource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;

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
    private HikariDataSource ds;

    @PostConstruct
    public void init() {
        var usageStats = proxyProperties.getUsageStats();
        var baseURL = usageStats.getUrl().getJdbc();
        var username = usageStats.getUsername();
        var password = usageStats.getPassword();
        ds = new HikariDataSource() {{
            setJdbcUrl(baseURL);
            setUsername(username);
            setPassword(password);
            addDataSourceProperty("useJDBCCompliantTimezoneShift", "true");
            addDataSourceProperty("serverTimezone", "UTC");
        }};

        var hikari = usageStats.getHikari();
        var connectionTimeout = hikari.getConnectionTimeout();
        if (!connectionTimeout.isNegative()) {
            ds.setConnectionTimeout(connectionTimeout.toMillis());
        }

        var idleTimeout = hikari.getIdleTimeout();
        if (!idleTimeout.isNegative()) {
            ds.setIdleTimeout(idleTimeout.toMillis());
        }

        var maxLifetime = hikari.getMaxLifetime();
        if (!maxLifetime.isNegative()) {
            ds.setMaxLifetime(maxLifetime.toMillis());
        }

        var minimumIdle = hikari.getMinimumIdle();
        if (minimumIdle >= 0) {
            ds.setMinimumIdle(minimumIdle);
        }

        var maximumPoolSize = hikari.getMaximumPoolSize();
        if (maximumPoolSize >= 0) {
            ds.setMaximumPoolSize(maximumPoolSize);
        }
    }

    @Override
    protected void writeToDb(long timestamp, String userId, String type, String specId, String info) throws IOException {
        var identifier = proxyProperties.getIdentifierValue();
        var sql = "INSERT INTO event(event_time, username, type, specid, identifier, info) VALUES (?,?,?,?,?,?)";
        try (var con = ds.getConnection()) {
            try (var stmt = con.prepareStatement(sql)) {
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

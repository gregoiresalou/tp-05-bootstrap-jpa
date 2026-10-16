package org.sebsy.compta.infra;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

/**
 * The application's single connection pool.
 *
 * <p>One {@code DataSource} for the whole JVM: building a pool opens connections, and a pool
 * built per call defeats its own purpose. This project has no injection container, so a
 * static holder is the honest form; an abstraction here would be ceremony.
 */
public final class Db {

    private static final HikariDataSource POOL = build();

    private Db() {
    }

    public static DataSource dataSource() {
        return POOL;
    }

    /** Closes the pool. Call once, when the application stops. */
    public static void close() {
        POOL.close();
    }

    private static HikariDataSource build() {
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("compta-pool");
        cfg.setJdbcUrl(Env.required("DB_URL"));
        cfg.setUsername(Env.required("DB_USER"));
        cfg.setPassword(Env.required("DB_PASSWORD"));

        // A chosen size, not the default: a pool wider than what the server processes in
        // parallel only moves the queue from the client to the database.
        cfg.setMaximumPoolSize(10);
        // A caller that cannot get a connection within 3 s gets an error, not an endless wait.
        cfg.setConnectionTimeout(3_000);
        // Logs the borrowing stack trace of any connection held more than 20 s: it turns
        // "the application froze" into "this line leaked a connection".
        cfg.setLeakDetectionThreshold(20_000);

        return new HikariDataSource(cfg);
    }
}

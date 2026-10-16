package org.sebsy.compta.dao.jdbc;

import org.sebsy.compta.dao.PersistenceFailure;
import org.sebsy.compta.infra.Db;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Transaction boundary of the JDBC DAOs.
 *
 * <p>The use case decides when to commit, through {@link #call} or {@link #run}; a DAO never
 * does, otherwise an order and its lines could not be written atomically. A DAO joins the
 * transaction of its thread through {@link #with}, without a {@code Connection} parameter
 * that would put {@code java.sql} back into its public contract.
 *
 * <p>This is what {@code @Transactional} does in Spring and
 * {@code EntityManagerFactory.runInTransaction} in Jakarta Persistence 3.2.
 */
public final class Tx {

    /** Connection of the transaction running on this thread, if any. */
    private static final ThreadLocal<Connection> CURRENT = new ThreadLocal<>();

    private Tx() {
    }

    @FunctionalInterface
    public interface SqlWork<R> {
        R apply(Connection connection) throws SQLException;
    }

    /** Runs {@code work} in one transaction and returns its result. */
    public static <R> R call(SqlWork<R> work) {
        if (CURRENT.get() != null) {
            throw new IllegalStateException("a transaction is already running on this thread");
        }
        try (Connection cnx = Db.dataSource().getConnection()) {
            cnx.setAutoCommit(false);
            CURRENT.set(cnx);
            try {
                R result = work.apply(cnx);
                cnx.commit();
                return result;
            } catch (SQLException | RuntimeException failure) {
                // Before propagating, and explicitly: JDBC leaves close() on an active
                // transaction implementation-defined. HikariCP happens to roll back on return;
                // this code does not rely on it. A failing rollback must not hide the cause.
                try {
                    cnx.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            } finally {
                CURRENT.remove();
            }
            // close() returns the connection to the pool, which restores auto-commit.
        } catch (SQLException e) {
            throw new PersistenceFailure("transaction failed", e);
        }
    }

    /** Runs {@code work} in one transaction, with no result. */
    public static void run(SqlWork<Void> work) {
        call(work);
    }

    /**
     * For the DAOs: joins the transaction of this thread when there is one (and leaves its
     * connection open, it belongs to {@link #call}); otherwise borrows a connection of its
     * own, in auto-commit. Wraps any SQLException, so that no caller imports java.sql.
     */
    static <R> R with(String description, SqlWork<R> work) {
        Connection ambient = CURRENT.get();
        try {
            if (ambient != null) {
                return work.apply(ambient);
            }
            try (Connection cnx = Db.dataSource().getConnection()) {
                return work.apply(cnx);
            }
        } catch (SQLException e) {
            throw new PersistenceFailure(description + " failed", e);
        }
    }
}

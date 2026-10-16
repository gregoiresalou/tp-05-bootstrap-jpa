package org.sebsy.compta.dao;

/**
 * Technical failure of the persistence layer, unchecked.
 *
 * <p>It keeps {@code java.sql} out of the rest of the application: a DAO declaring
 * {@code throws SQLException} forces every caller to import the JDBC API. The outcomes a
 * caller can act on ("not found", "0 rows") are return values, not exceptions.
 */
public class PersistenceFailure extends RuntimeException {

    public PersistenceFailure(String message, Throwable cause) {
        super(message, cause);
    }
}

package org.sebsy.compta.app;

import org.sebsy.compta.infra.Db;
import org.sebsy.compta.infra.Env;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * Times 100 borrow/return cycles: a fresh DriverManager connection each time, then the pool.
 *
 * <p>The figures depend on the machine and on Docker; the ratio between the two is the point.
 */
public final class MesurePool {

    private static final int N = 100;

    public static void main(String[] args) throws SQLException {
        String url = Env.required("DB_URL");
        String user = Env.required("DB_USER");
        String password = Env.required("DB_PASSWORD");

        long debut = System.nanoTime();
        for (int i = 0; i < N; i++) {
            try (Connection cnx = DriverManager.getConnection(url, user, password)) {
                // Opening is the cost being measured: TCP, handshake, authentication.
            }
        }
        long driverManager = System.nanoTime() - debut;

        // Starting the pool opens its first connections: done before the timer on purpose,
        // since an application pays it once, at startup.
        DataSource ds = Db.dataSource();
        try (Connection cnx = ds.getConnection()) {
            // Warm-up borrow.
        }

        debut = System.nanoTime();
        for (int i = 0; i < N; i++) {
            try (Connection cnx = ds.getConnection()) {
                // close() hands the connection back to the pool; nothing is opened here.
            }
        }
        long pool = System.nanoTime() - debut;

        System.out.printf("DriverManager : %d connexions en %d ms, soit %d µs par connexion%n",
                N, driverManager / 1_000_000, driverManager / 1_000 / N);
        System.out.printf("Pool Hikari   : %d emprunts en %d ms, soit %d µs par emprunt%n",
                N, pool / 1_000_000, pool / 1_000 / N);
        System.out.printf("Rapport       : x%.0f%n", (double) driverManager / pool);
        Db.close();
    }
}

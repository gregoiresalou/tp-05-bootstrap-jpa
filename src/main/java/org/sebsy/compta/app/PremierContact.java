package org.sebsy.compta.app;

import org.sebsy.compta.infra.Db;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * First round trip: the connection is borrowed from the pool.
 */
public final class PremierContact {

    public static void main(String[] args) throws SQLException {
        try (Connection cnx = Db.dataSource().getConnection();
             Statement st = cnx.createStatement()) {

            // The declared type is the java.sql interface; the object behind it is a pool
            // proxy wrapping the driver's connection.
            System.out.println("Classe de la connexion : " + cnx.getClass().getName());
            System.out.println("Classe du pilote       : " + cnx.unwrap(Connection.class).getClass().getName());
            System.out.println("Serveur                : " + cnx.getMetaData().getDatabaseProductVersion());

            // Constant SQL, no value from outside: a plain Statement is enough here.
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM fournisseur")) {
                rs.next();
                System.out.println("Fournisseurs           : " + rs.getInt(1));
            }
            try (ResultSet rs = st.executeQuery("SELECT id, nom, ville FROM fournisseur ORDER BY id")) {
                while (rs.next()) {
                    System.out.printf("  %d  %-24s %s%n", rs.getLong("id"), rs.getString("nom"), rs.getString("ville"));
                }
            }
        }
        // Not in a finally block: if the pool failed to start, a finally touching Db again
        // would replace the real cause with a NoClassDefFoundError.
        Db.close();
    }
}

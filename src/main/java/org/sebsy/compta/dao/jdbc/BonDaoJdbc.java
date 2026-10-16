package org.sebsy.compta.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Purchase orders.
 *
 * <p>No business interface here: an order and its lines form one aggregate, and this class
 * shows the shape a hand-written layer really has. {@link #insererEntete} and
 * {@link #insererLigne} never commit; {@link #creerAvecLignes} opens the transaction they join.
 */
public class BonDaoJdbc {

    /** One line of an order, flattened with its article. */
    public record LigneBrute(long idArticle, String ref, String designation, BigDecimal prix, int quantite) {
    }

    /**
     * An order, its supplier and its lines. {@code statut} is read as a String: no enum maps it yet.
     */
    public record BonComplet(long id, int numero, LocalDateTime dateCommande, String statut,
                             Integer delai, String fournisseur, String ville, List<LigneBrute> lignes) {
    }

    /**
     * Inserts an order header and returns its generated id. {@code statut} keeps its database
     * default ({@code BROUILLON}).
     */
    public long insererEntete(int numero, LocalDateTime date, long idFournisseur) {
        return Tx.with("insererEntete", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement(
                    "INSERT INTO bon (numero, date_cmde, id_fou) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, numero);
                // JDBC 4.2 binds java.time directly: no java.sql.Timestamp.
                ps.setObject(2, date);
                ps.setLong(3, idFournisseur);
                ps.executeUpdate();
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    if (!cles.next()) {
                        throw new SQLException("no generated key returned for the inserted order");
                    }
                    return cles.getLong(1);
                }
            }
        });
    }

    /** Inserts one line. Joins the ambient transaction, never commits. */
    public void insererLigne(long idBon, long idArticle, int quantite) {
        Tx.with("insererLigne", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement(
                    "INSERT INTO compo (id_art, id_bon, qte) VALUES (?, ?, ?)")) {
                ps.setLong(1, idArticle);
                ps.setLong(2, idBon);
                ps.setInt(3, quantite);
                ps.executeUpdate();
                return null;
            }
        });
    }

    /**
     * Creates an order and all its lines atomically: either everything is committed, or the
     * rollback leaves the database as it was. In auto-commit, a failure on the third line
     * would leave a committed order with two lines, and nobody would be told.
     */
    public long creerAvecLignes(int numero, LocalDateTime date, long idFournisseur,
                                Map<Long, Integer> quantitesParArticle) {
        return Tx.call(cnx -> {
            long idBon = insererEntete(numero, date, idFournisseur);
            for (Map.Entry<Long, Integer> ligne : quantitesParArticle.entrySet()) {
                insererLigne(idBon, ligne.getKey(), ligne.getValue());
            }
            return idBon;
        });
    }

    /** Date of an order, read back as java.time. */
    public Optional<LocalDateTime> dateCommande(long idBon) {
        return Tx.with("dateCommande", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement("SELECT date_cmde FROM bon WHERE id = ?")) {
                ps.setLong(1, idBon);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next()
                            ? Optional.of(rs.getObject("date_cmde", LocalDateTime.class))
                            : Optional.<LocalDateTime>empty();
                }
            }
        });
    }

    /** Delivery delay, empty when the order does not exist or is not delivered yet (NULL). */
    public Optional<Integer> delai(long idBon) {
        return Tx.with("delai", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement("SELECT delai FROM bon WHERE id = ?")) {
                ps.setLong(1, idBon);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.<Integer>empty();
                    }
                    // getInt would turn NULL ("not delivered yet") into 0 ("delivered in zero days").
                    return Optional.ofNullable(rs.getObject("delai", Integer.class));
                }
            }
        });
    }

    /**
     * Loads one order, its supplier, its lines and each line's article, by hand.
     *
     * <p>One statement with an inner and two outer joins, then what no join does: reading the
     * header repeated on every row once, and attaching the lines to it.
     */
    public Optional<BonComplet> chargerGrapheALaMain(long idBon) {
        return Tx.with("chargerGrapheALaMain", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement("""
                    SELECT b.id, b.numero, b.date_cmde, b.statut, b.delai,
                           f.nom AS fournisseur, f.ville,
                           a.id  AS id_art, a.ref, a.designation, a.prix,
                           c.qte
                    FROM   bon b
                           JOIN      fournisseur f ON f.id = b.id_fou
                           LEFT JOIN compo       c ON c.id_bon = b.id
                           LEFT JOIN article     a ON a.id = c.id_art
                    WHERE  b.id = ?
                    ORDER  BY a.ref
                    """)) {
                ps.setLong(1, idBon);
                try (ResultSet rs = ps.executeQuery()) {
                    BonComplet entete = null;
                    Map<Long, LigneBrute> lignes = new LinkedHashMap<>();
                    while (rs.next()) {
                        // The header is repeated on every row of the join: built once.
                        if (entete == null) {
                            entete = new BonComplet(
                                    rs.getLong("id"),
                                    rs.getInt("numero"),
                                    rs.getObject("date_cmde", LocalDateTime.class),
                                    rs.getString("statut"),
                                    rs.getObject("delai", Integer.class),
                                    rs.getString("fournisseur"),
                                    rs.getString("ville"),
                                    new ArrayList<>());
                        }
                        // An order without lines yields one row whose compo/article columns are
                        // all NULL. getLong returns 0 for NULL, hence wasNull() to avoid a phantom line.
                        long idArticle = rs.getLong("id_art");
                        if (!rs.wasNull()) {
                            lignes.putIfAbsent(idArticle, new LigneBrute(
                                    idArticle,
                                    rs.getString("ref"),
                                    rs.getString("designation"),
                                    rs.getBigDecimal("prix"),
                                    rs.getInt("qte")));
                        }
                    }
                    if (entete == null) {
                        return Optional.<BonComplet>empty();
                    }
                    entete.lignes().addAll(lignes.values());
                    return Optional.of(entete);
                }
            }
        });
    }
}

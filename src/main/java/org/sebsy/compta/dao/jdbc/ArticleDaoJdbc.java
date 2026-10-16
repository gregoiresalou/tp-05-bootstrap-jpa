package org.sebsy.compta.dao.jdbc;

import org.sebsy.compta.dao.ArticleDao;
import org.sebsy.compta.dao.PersistenceFailure;
import org.sebsy.compta.domain.Article;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link ArticleDao}. Arithmetic that belongs to the database stays there. */
public class ArticleDaoJdbc implements ArticleDao {

    private static final String COLONNES = "id, ref, designation, prix, actif";

    @Override
    public Optional<Article> parId(long id) {
        return Tx.with("parId", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement(
                    "SELECT " + COLONNES + " FROM article WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(versArticle(rs)) : Optional.<Article>empty();
                }
            }
        });
    }

    @Override
    public Optional<Article> parRef(String ref) {
        return Tx.with("parRef", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement(
                    "SELECT " + COLONNES + " FROM article WHERE ref = ?")) {
                ps.setString(1, ref);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(versArticle(rs)) : Optional.<Article>empty();
                }
            }
        });
    }

    @Override
    public List<Article> parFournisseur(long idFournisseur) {
        return Tx.with("parFournisseur", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement(
                    "SELECT " + COLONNES + " FROM article WHERE id_fou = ? ORDER BY ref")) {
                ps.setLong(1, idFournisseur);
                return lireTout(ps);
            }
        });
    }

    @Override
    public List<Article> plusCherQue(BigDecimal seuil) {
        return Tx.with("plusCherQue", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement(
                    "SELECT " + COLONNES + " FROM article WHERE prix > ? ORDER BY prix DESC")) {
                // setBigDecimal, not setDouble: the column is DECIMAL(7,2) and the value must
                // stay exact all the way down.
                ps.setBigDecimal(1, seuil);
                return lireTout(ps);
            }
        });
    }

    @Override
    public Article creer(Article article, long idFournisseur) {
        return Tx.with("creer", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement("""
                    INSERT INTO article (ref, designation, prix, actif, id_fou)
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, article.getRef());
                ps.setString(2, article.getDesignation());
                ps.setBigDecimal(3, article.getPrix());
                ps.setBoolean(4, article.isActif());
                ps.setLong(5, idFournisseur);
                ps.executeUpdate();

                try (ResultSet cles = ps.getGeneratedKeys()) {
                    if (!cles.next()) {
                        throw new SQLException("no generated key returned for the inserted article");
                    }
                    return new Article(cles.getLong(1), article.getRef(), article.getDesignation(),
                            article.getPrix(), article.isActif());
                }
            }
        });
    }

    @Override
    public int remiser(String motifDesignation, BigDecimal facteur) {
        if (facteur.signum() <= 0) {
            throw new IllegalArgumentException("facteur must be > 0");
        }
        return Tx.with("remiser", cnx -> {
            // One statement, computed by the database: loading the rows to multiply them in
            // Java and writing them back would cost one round trip per row.
            try (PreparedStatement ps = cnx.prepareStatement("""
                    UPDATE article
                    SET    prix = ROUND(prix * ?, 2)
                    WHERE  designation LIKE ?
                    """)) {
                ps.setBigDecimal(1, facteur);
                // The wildcards belong to the bound VALUE, never to the SQL text.
                ps.setString(2, "%" + motifDesignation + "%");
                return ps.executeUpdate();
            }
        });
    }

    @Override
    public Optional<BigDecimal> prixMoyen() {
        return Tx.with("prixMoyen", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement(
                    "SELECT AVG(prix) AS moyenne FROM article WHERE actif = TRUE");
                 ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new PersistenceFailure("AVG returned no row, which cannot happen", null);
                }
                // AVG over an empty set is SQL NULL. getBigDecimal returns null for it —
                // unlike getDouble, which would return 0.0 and turn "no data" into "zero".
                return Optional.ofNullable(rs.getBigDecimal("moyenne"));
            }
        });
    }

    private static List<Article> lireTout(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Article> resultat = new ArrayList<>();
            while (rs.next()) {
                resultat.add(versArticle(rs));
            }
            return resultat;
        }
    }

    private static Article versArticle(ResultSet rs) throws SQLException {
        return new Article(
                rs.getLong("id"),
                rs.getString("ref"),
                rs.getString("designation"),
                rs.getBigDecimal("prix"),
                rs.getBoolean("actif"));
    }
}

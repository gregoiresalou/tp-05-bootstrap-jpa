package org.sebsy.compta.dao.jdbc;

import org.sebsy.compta.dao.FournisseurDao;
import org.sebsy.compta.domain.Adresse;
import org.sebsy.compta.domain.Fournisseur;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link FournisseurDao}.
 *
 * <p>The connection comes from {@link Tx#with}, which joins the caller's transaction when there
 * is one and wraps any SQLException into a PersistenceFailure.
 */
public class FournisseurDaoJdbc implements FournisseurDao {

    private static final String COLONNES = "id, nom, rue, code_postal, ville";

    @Override
    public List<Fournisseur> toutes() {
        return Tx.with("toutes", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement("SELECT " + COLONNES + " FROM fournisseur ORDER BY id")) {
                return lireTout(ps);
            }
        });
    }

    @Override
    public Optional<Fournisseur> parId(long id) {
        return Tx.with("parId", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement("SELECT " + COLONNES + " FROM fournisseur WHERE id = ?")) {
                ps.setLong(1, id);
                return lireUn(ps);
            }
        });
    }

    @Override
    public Optional<Fournisseur> parNom(String nom) {
        return Tx.with("parNom", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement("SELECT " + COLONNES + " FROM fournisseur WHERE nom = ?")) {
                ps.setString(1, nom);
                return lireUn(ps);
            }
        });
    }

    @Override
    public List<Fournisseur> parVille(String ville) {
        return Tx.with("parVille", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement(
                    "SELECT " + COLONNES + " FROM fournisseur WHERE ville = ? ORDER BY nom")) {
                ps.setString(1, ville);
                return lireTout(ps);
            }
        });
    }

    @Override
    public Fournisseur creer(Fournisseur fournisseur) {
        return Tx.with("creer", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement("""
                    INSERT INTO fournisseur (nom, rue, code_postal, ville)
                    VALUES (?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS)) {
                Adresse adresse = fournisseur.getAdresse();
                ps.setString(1, fournisseur.getNom());
                ps.setString(2, adresse.rue());
                ps.setString(3, adresse.codePostal());
                ps.setString(4, adresse.ville());
                ps.executeUpdate();
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    if (!cles.next()) {
                        throw new SQLException("no generated key returned for the inserted supplier");
                    }
                    return new Fournisseur(cles.getLong(1), fournisseur.getNom(), adresse);
                }
            }
        });
    }

    @Override
    public boolean renommer(long id, String nouveauNom) {
        return Tx.with("renommer", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement("UPDATE fournisseur SET nom = ? WHERE id = ?")) {
                ps.setString(1, nouveauNom);
                ps.setLong(2, id);
                return ps.executeUpdate() == 1;
            }
        });
    }

    @Override
    public boolean supprimer(long id) {
        return Tx.with("supprimer", cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM fournisseur WHERE id = ?")) {
                ps.setLong(1, id);
                // A supplier that still has articles violates fk_article_fournisseur (RESTRICT):
                // the database enforces it, and it surfaces as a PersistenceFailure.
                return ps.executeUpdate() == 1;
            }
        });
    }

    private static Optional<Fournisseur> lireUn(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(versFournisseur(rs)) : Optional.empty();
        }
    }

    private static List<Fournisseur> lireTout(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Fournisseur> resultat = new ArrayList<>();
            while (rs.next()) {
                resultat.add(versFournisseur(rs));
            }
            return resultat;
        }
    }

    /** Columns read by name: an index breaks silently the day a column is added in the middle. */
    private static Fournisseur versFournisseur(ResultSet rs) throws SQLException {
        return new Fournisseur(
                rs.getLong("id"),
                rs.getString("nom"),
                new Adresse(rs.getString("rue"), rs.getString("code_postal"), rs.getString("ville")));
    }
}

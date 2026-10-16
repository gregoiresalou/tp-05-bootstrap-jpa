package org.sebsy.compta.app;

import org.sebsy.compta.dao.PersistenceFailure;
import org.sebsy.compta.dao.jdbc.BonDaoJdbc;
import org.sebsy.compta.dao.jdbc.Tx;
import org.sebsy.compta.infra.Db;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An order whose third line references a missing article must leave
 * nothing behind. Run with a numero of your own (first argument) to avoid UNIQUE(numero)
 * collisions between runs.
 */
public final class ScenarioBon {

    public static void main(String[] args) {
        int numero = args.length > 0 ? Integer.parseInt(args[0]) : 9001;
        var bons = new BonDaoJdbc();

        long idBon = bons.creerAvecLignes(numero, LocalDateTime.now(), 1L, Map.of(1L, 3, 5L, 2));
        System.out.println("nominal  : bon " + idBon + ", " + lignes(idBon) + " lignes, date relue "
                + bons.dateCommande(idBon).orElseThrow());

        // Insertion order matters: the missing article (999) comes third.
        Map<Long, Integer> commande = new LinkedHashMap<>();
        commande.put(1L, 3);
        commande.put(5L, 2);
        commande.put(999L, 1);
        try {
            bons.creerAvecLignes(numero + 1, LocalDateTime.now(), 1L, commande);
            System.out.println("échec    : aucune exception, ce n'est pas normal");
        } catch (PersistenceFailure e) {
            System.out.println("échec    : " + e.getMessage() + " <- " + e.getCause().getMessage());
        }
        System.out.println("en base  : " + bonsNumero(numero + 1) + " bon(s) numéro " + (numero + 1)
                + ", " + lignesDuNumero(numero + 1) + " ligne(s)");

        supprimer(numero);
        supprimer(numero + 1);
        Db.close();
    }

    private static int lignes(long idBon) {
        return compter("SELECT COUNT(*) FROM compo WHERE id_bon = ?", idBon);
    }

    private static int bonsNumero(int numero) {
        return compter("SELECT COUNT(*) FROM bon WHERE numero = ?", numero);
    }

    private static int lignesDuNumero(int numero) {
        return compter("SELECT COUNT(*) FROM compo c JOIN bon b ON b.id = c.id_bon WHERE b.numero = ?", numero);
    }

    private static int compter(String sql, long valeur) {
        return Tx.call(cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setLong(1, valeur);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }

    // fk_compo_bon is ON DELETE CASCADE: deleting the header deletes its lines.
    private static void supprimer(int numero) {
        Tx.run(cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM bon WHERE numero = ?")) {
                ps.setInt(1, numero);
                ps.executeUpdate();
            }
            return null;
        });
    }
}

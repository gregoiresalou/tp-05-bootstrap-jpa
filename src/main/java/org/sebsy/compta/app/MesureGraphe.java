package org.sebsy.compta.app;

import org.sebsy.compta.dao.jdbc.BonDaoJdbc;
import org.sebsy.compta.infra.Db;

/**
 * Prints the two test graphs, then times 100 loads of order 1.
 * Count the statements in mysql.general_log while it runs (procedure in env/README.md).
 */
public final class MesureGraphe {

    private static final int N = 100;

    public static void main(String[] args) {
        var bons = new BonDaoJdbc();
        System.out.println("bon 1 : " + bons.chargerGrapheALaMain(1).orElseThrow());
        System.out.println("bon 7 : " + bons.chargerGrapheALaMain(7).orElseThrow());

        long debut = System.nanoTime();
        for (int i = 0; i < N; i++) {
            bons.chargerGrapheALaMain(1);
        }
        long total = System.nanoTime() - debut;
        System.out.printf("%d chargements du bon 1 : %d ms, soit %d µs par chargement%n",
                N, total / 1_000_000, total / 1_000 / N);
        Db.close();
    }
}

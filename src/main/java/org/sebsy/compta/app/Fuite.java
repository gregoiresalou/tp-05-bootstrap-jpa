package org.sebsy.compta.app;

import org.sebsy.compta.infra.Db;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Borrows connections and never gives them back.
 *
 * <p>The pool holds 10 connections and waits 3 s for a free one, so the 11th request fails.
 * The leak report only comes 20 s after each borrowing (leakDetectionThreshold): the program
 * therefore stays alive 25 s after the failure, or it would exit before any report.
 */
public final class Fuite {

    public static void main(String[] args) throws InterruptedException {
        List<Connection> jamaisRendues = new ArrayList<>();
        long debut = System.nanoTime();
        for (int i = 1; i <= 200; i++) {
            try {
                jamaisRendues.add(Db.dataSource().getConnection());
                System.out.printf("[t+%5d ms] itération %d : connexion obtenue%n", ms(debut), i);
            } catch (SQLException e) {
                System.out.printf("[t+%5d ms] itération %d : %s%n", ms(debut), i, e);
                break;
            }
        }
        Thread.sleep(25_000);
        System.out.printf("[t+%5d ms] fin%n", ms(debut));
    }

    private static long ms(long debut) {
        return (System.nanoTime() - debut) / 1_000_000;
    }
}

package org.sebsy.compta.app;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.sebsy.compta.domain.Fournisseur;
import org.sebsy.compta.infra.Jpa;

public class EssaiPortees {
    public static void main(String[] args) {
        SessionFactory factory = Jpa.factory();

        System.out.println("--- 1 transaction, 2 lectures ---");
        factory.getStatistics().clear();

        EntityManager em1 = factory.createEntityManager();
        em1.getTransaction().begin();

        Fournisseur f1 = em1.find(Fournisseur.class, 1L);
        Fournisseur f2 = em1.find(Fournisseur.class, 1L);

        System.out.println("Même référence (==) ? " + (f1 == f2));

        em1.getTransaction().commit();
        em1.close();
        System.out.println("Nombre de requêtes : " + factory.getStatistics().getPrepareStatementCount());

        System.out.println("\n--- 2 transactions successives ---");
        factory.getStatistics().clear();

        EntityManager em2 = factory.createEntityManager();
        em2.getTransaction().begin();
        Fournisseur f3 = em2.find(Fournisseur.class, 1L);
        em2.getTransaction().commit();
        em2.close();

        EntityManager em3 = factory.createEntityManager();
        em3.getTransaction().begin();
        Fournisseur f4 = em3.find(Fournisseur.class, 1L);
        em3.getTransaction().commit();
        em3.close();

        System.out.println("Même référence (==) ? " + (f3 == f4));
        System.out.println("Même contenu (equals) ? " + f3.equals(f4));
        System.out.println("Nombre de requêtes : " + factory.getStatistics().getPrepareStatementCount());

        factory.close();
    }
}
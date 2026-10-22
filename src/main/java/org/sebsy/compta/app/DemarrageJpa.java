package org.sebsy.compta.app;

import org.hibernate.SessionFactory;
import org.sebsy.compta.domain.Fournisseur;
import org.sebsy.compta.infra.Jpa;

/** Étape 2 : démarre Hibernate (validate), lit le fournisseur 4, ferme la fabrique. */
public final class DemarrageJpa {

    public static void main(String[] args) {
        // Avant le try : si la construction échoue, c'est l'erreur d'Hibernate qu'on voit.
        SessionFactory factory = Jpa.factory();
        try {
            System.out.println("Classe de la fabrique : " + factory.getClass().getName());

            Fournisseur f = factory.callInTransaction(em -> em.find(Fournisseur.class, 4L));

            System.out.println(f);
            System.out.println("estComplete() : " + f.getAdresse().estComplete());
        } finally {
            factory.close();
        }
    }
}
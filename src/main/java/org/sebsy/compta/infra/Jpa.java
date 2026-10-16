package org.sebsy.compta.infra;

import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;
import org.sebsy.compta.domain.Fournisseur;

public final class Jpa {

    private static SessionFactory factory;   // l'unique SessionFactory

    private Jpa() { }                        // on interdit de faire "new Jpa()"

    public static synchronized SessionFactory factory() {
        if (factory == null) {
            factory = new HibernatePersistenceConfiguration("compta")
                    .managedClasses(Fournisseur.class)
                    .jdbcUrl(Env.required("DB_URL"))
                    .jdbcUsername(Env.required("DB_USER"))
                    .jdbcPassword(Env.required("DB_PASSWORD"))
                    .jdbcPoolSize(10)
                    .schemaToolingAction(Action.VALIDATE)
                    .property("hibernate.generate_statistics", true)
                    .createEntityManagerFactory();
        }
        return factory;
    }

    public static synchronized void close() {
        if (factory != null) {
            factory.close();
            factory = null;
        }
    }
}
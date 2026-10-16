package org.sebsy.compta.infra;

/**
 * Reads mandatory environment variables.
 *
 * <p>Credentials never appear in the code or in a versioned file: they come from the
 * environment of the run. A missing variable fails at once, with a message that names it
 * and says where to declare it, instead of a driver error three layers further down.
 */
public final class Env {

    private Env() {
    }

    public static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("""
                    Variable d'environnement %s absente.
                    Déclarez DB_URL, DB_USER et DB_PASSWORD dans la configuration d'exécution de l'IDE :
                    une variable exportée dans un terminal n'est pas vue par un programme lancé depuis l'IDE."""
                    .formatted(name));
        }
        return value;
    }
}

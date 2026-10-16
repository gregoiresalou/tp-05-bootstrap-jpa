package org.sebsy.compta.domain;

import java.util.Objects;

/**
 * A supplier's postal address: a value, not an entity.
 *
 * <p>Two addresses with the same street, postcode and town are the same address, which is
 * exactly the equality a record provides. It maps to three columns of {@code fournisseur}.
 *
 * <p>{@code rue} and {@code codePostal} are nullable (rows inherited from the SQL module do not
 * have them); {@code ville} is NOT NULL in the table, so it is mandatory here too.
 */
public record Adresse(String rue, String codePostal, String ville) {

    public Adresse {
        Objects.requireNonNull(ville, "ville is mandatory (column is NOT NULL)");
    }

    /** The frequent case where only the town is known. */
    public static Adresse ville(String ville) {
        return new Adresse(null, null, ville);
    }

    public boolean estComplete() {
        return rue != null && codePostal != null;
    }
}

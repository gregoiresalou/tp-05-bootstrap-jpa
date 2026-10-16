package org.sebsy.compta.dao;

import org.sebsy.compta.domain.Fournisseur;
import java.util.List;
import java.util.Optional;

/**
 * Supplier persistence contract.
 *
 * <p>No {@code java.sql} type, no {@code SQLException}, no SQL: a JDBC implementation, a JPA
 * one or an in-memory fake can stand behind it without the calling code changing.
 * {@code Optional} rather than {@code null}: "not found" is a normal outcome.
 */
public interface FournisseurDao {

    Optional<Fournisseur> parId(long id);

    Optional<Fournisseur> parNom(String nom);

    List<Fournisseur> parVille(String ville);

    List<Fournisseur> toutes();

    /** Inserts and returns the supplier with its generated identifier. */
    Fournisseur creer(Fournisseur fournisseur);

    /** @return false when no supplier has this identifier: not an error. */
    boolean renommer(long id, String nouveauNom);

    /** @return false when nothing was deleted. Fails when articles still reference it. */
    boolean supprimer(long id);
}

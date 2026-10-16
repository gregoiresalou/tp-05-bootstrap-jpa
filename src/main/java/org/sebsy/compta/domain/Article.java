package org.sebsy.compta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A catalogue item.
 *
 * <p>Not final, protected no-argument constructor, {@code Long} id: the same choices as
 * {@link Fournisseur}, for the same reasons.
 */
public class Article {

    private Long id;
    private String ref;
    private String designation;
    /** BigDecimal, never double: binary floating point cannot hold 0.10 exactly. */
    private BigDecimal prix;
    private boolean actif = true;

    protected Article() {
    }

    public Article(String ref, String designation, BigDecimal prix) {
        this.ref = Objects.requireNonNull(ref, "ref is mandatory");
        this.designation = Objects.requireNonNull(designation, "designation is mandatory");
        setPrix(prix);
    }

    public Article(Long id, String ref, String designation, BigDecimal prix, boolean actif) {
        this(ref, designation, prix);
        this.id = id;
        this.actif = actif;
    }

    public Long getId() {
        return id;
    }

    public String getRef() {
        return ref;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = Objects.requireNonNull(designation);
    }

    public BigDecimal getPrix() {
        return prix;
    }

    /**
     * Refuses a negative price and normalises to 2 decimals, the scale of DECIMAL(7,2): the
     * object then holds exactly what the database will store. The CHECK constraint remains
     * the guarantee; this check only fails earlier, with a clearer message.
     */
    public void setPrix(BigDecimal prix) {
        Objects.requireNonNull(prix, "prix is mandatory");
        if (prix.signum() < 0) {
            throw new IllegalArgumentException("prix must be >= 0 (CHECK constraint ck_article_prix)");
        }
        this.prix = prix.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    /** Identity on {@code ref}: UNIQUE, NOT NULL, never updated. */
    @Override
    public boolean equals(Object other) {
        return other instanceof Article autre && ref.equals(autre.getRef());
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }

    @Override
    public String toString() {
        return "Article[id=%s, ref=%s, prix=%s]".formatted(id, ref, prix);
    }
}

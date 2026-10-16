package org.sebsy.compta.dao;

import org.sebsy.compta.domain.Article;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Catalogue item persistence contract. Same rules as {@link FournisseurDao}. */
public interface ArticleDao {

    Optional<Article> parId(long id);

    Optional<Article> parRef(String ref);

    List<Article> parFournisseur(long idFournisseur);

    /** Items whose price is strictly greater than the given threshold, dearest first. */
    List<Article> plusCherQue(BigDecimal seuil);

    Article creer(Article article, long idFournisseur);

    /**
     * Applies a discount to every item whose designation matches, in ONE statement.
     *
     * @return the number of rows affected — 0 is information, not a failure.
     */
    int remiser(String motifDesignation, BigDecimal facteur);

    /** Average price, computed BY THE DATABASE. Empty when the catalogue is empty. */
    Optional<BigDecimal> prixMoyen();
}

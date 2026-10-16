package org.sebsy.compta.app;

import org.sebsy.compta.dao.ArticleDao;
import org.sebsy.compta.dao.jdbc.ArticleDaoJdbc;
import org.sebsy.compta.dao.jdbc.Tx;
import org.sebsy.compta.domain.Article;
import org.sebsy.compta.infra.Db;
import java.math.BigDecimal;
import java.sql.PreparedStatement;

/** The seven ArticleDao methods, each called once. The database is left as it was found. */
public final class EssaiArticles {

    public static void main(String[] args) {
        ArticleDao articles = new ArticleDaoJdbc();

        System.out.println("parId(1)             : " + articles.parId(1).orElseThrow());
        System.out.println("parRef(F02)          : " + articles.parRef("F02").orElseThrow());
        System.out.println("parFournisseur(4)    : " + articles.parFournisseur(4));
        System.out.println("plusCherQue(50)      : " + articles.plusCherQue(new BigDecimal("50")));
        System.out.println("prixMoyen()          : " + articles.prixMoyen().orElseThrow());

        Article cree = articles.creer(new Article("Z99", "Marteau de test", new BigDecimal("9.9")), 4L);
        System.out.println("creer()              : " + cree);
        System.out.println("remiser(test, 0.90)  : " + articles.remiser("de test", new BigDecimal("0.90")) + " ligne(s)");
        System.out.println("après remise         : " + articles.parId(cree.getId()).orElseThrow());
        System.out.println("remiser(zzz, 0.90)   : " + articles.remiser("zzz", new BigDecimal("0.90")) + " ligne(s)");

        Tx.run(cnx -> {
            try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM article WHERE id = ?")) {
                ps.setLong(1, cree.getId());
                ps.executeUpdate();
            }
            return null;
        });
        Db.close();
    }
}

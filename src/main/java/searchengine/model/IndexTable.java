package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "search_index")
public class IndexTable {

    @Id
    @Getter
    @Setter
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private PageTable pageTable;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private LemmaTable lemmaTable;

    @Getter
    @Setter
    @Column(name = "rank_lemma", nullable = false)
    private float rankLemma;

    @Override
    public String toString() {
        return "IndexTable {" + "id = " + id + ", " +
                "page_id = '" + pageTable + '\'' + ", " +
                "lemma_id = '" + lemmaTable + '\'' + ", " +
                "rank_lemma = " + rankLemma + "'}";
    }
}

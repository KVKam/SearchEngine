package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lemma")
public class LemmaTable {

    @Id
    @Getter
    @Setter
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteTable siteTable;

    @Getter
    @Setter
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Getter
    @Setter
    @Column(nullable = false)
    private int frequency;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "lemmaTable", fetch = FetchType.LAZY)
    private List<IndexTable> Indexes = new ArrayList<>();

    @Override
    public String toString() {
        return "LemmaTable {" + "id = " + id + ", " +
                "site_id = '" + siteTable + '\'' + ", " +
                "lemma = '" + lemma + '\'' + ", " +
                "frequency = " + frequency + '\'';
    }
}

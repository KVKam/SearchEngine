package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "site")
public class SiteTable {
    @Id
    @Getter
    @Setter
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private StatusType status;

    @Getter
    @Setter
    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Getter
    @Setter
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String  lastError;

    @Getter
    @Setter
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Getter
    @Setter
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL,  fetch = FetchType.LAZY, mappedBy = "siteTable", orphanRemoval = true)
    private List<PageTable> Pages = new ArrayList<>();

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "siteTable", orphanRemoval = true)
    private List<LemmaTable> Lemmas = new ArrayList<>();
    @Override
    public String toString() {
        return "SiteTable {" + "id = " + id + ", " +
                "status = '" + status + '\'' + ", " +
                "status_time = '" + statusTime + '\'' + ", " +
                "last_error = '" + lastError + '\'' + ", " +
                "url = '" + url + '\'' + ", " +
                "name = " + name + '\''+"}";
    }
}

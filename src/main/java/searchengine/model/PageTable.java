package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "page",
        indexes = {@Index(columnList = "path", name = "indx_path")})
public class PageTable {
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
    private String path;

    @Column(nullable = false)
    @Getter
    @Setter
    private int code;

    @Getter
    @Setter
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "pageTable", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<IndexTable> Indexes = new ArrayList<>();

    @Override
    public String toString() {
        return "PageTable {" + "id = " + id + ", " +
                "site_id = '" + siteTable + '\'' + ", " +
                "path = '" + path + '\'' + ", " +
                "code = '" + code + '\'' + ", " +
                "content = " + content + '\'';
    }
}

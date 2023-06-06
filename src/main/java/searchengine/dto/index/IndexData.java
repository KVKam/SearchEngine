package searchengine.dto.index;

import lombok.Data;
import searchengine.model.SiteTable;
@Data
public class IndexData {
    private SiteTable siteTable;
    private int parseTimeout;
    private String url;
    private boolean oneIndexing;

    public IndexData(SiteTable siteTable, int parseTimeout, String url) {
        this.siteTable = siteTable;
        this.parseTimeout = parseTimeout;
        this.url = url;
    }
}

package searchengine.parsing;

import org.jsoup.UncheckedIOException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveTask;

public class SiteMap extends RecursiveTask<StringBuilder> {
    private static CopyOnWriteArraySet<String> allIn = new CopyOnWriteArraySet<>();
    private final Logger logger = LoggerFactory.getLogger(SiteMap.class);
    private final String path;
    private final int parseTimeout;

    public SiteMap(String path, int parseTimeout) {
        this.path = path;
        this.parseTimeout = parseTimeout;
    }

    @Override
    protected StringBuilder compute() {
        StringBuilder siteMap = new StringBuilder(path + "\n");
        Set<SiteMap> listPath = new HashSet<>();
        getWay(listPath);
        for (SiteMap siteUrl : listPath) {
            siteMap.append(siteUrl.join());
        }
        return siteMap;
    }

    protected void getWay(Set<SiteMap> task) {
        if (getPool().isShutdown()) {
            return;
        }
        try {
            Document document = IndexingThread.connectPath(path, parseTimeout).get();
            Elements links = document.select("a[href]");
            for (Element link : links) {
                String cleanLink = normalizeLink(link.attr("abs:href"));
                String siteUrl = path.replace(path.substring(path.indexOf('/', 16) + 1), "").replace("www.", "");
                String regex = siteUrl + "[A-Za-z0-9/\\-_]*(.html)?/?";
                if (!allIn.contains(cleanLink)
                        && !cleanLink.isEmpty()
                        && cleanLink.matches(regex)
                ) {
                    allIn.add(cleanLink);
                    SiteMap siteMap = new SiteMap(cleanLink, parseTimeout);
                    task.add(siteMap);
                    siteMap.fork();
                }
            }
        } catch (IOException | InterruptedException | UncheckedIOException ex) {
            logger.error("Ошибка парсинга страницы " + ex + " " + path);
        }
    }

    private String normalizeLink(String link){
        link = link.trim().replace("www.", "").toLowerCase();
        link = (!(link.lastIndexOf("/") == (link.length() - 1)))
                ? link + "/" : link;
        link = !(link.lastIndexOf("//") == link.indexOf("//"))
                ? link.substring(0, link.lastIndexOf("//"))
                + link.substring(link.lastIndexOf("//") + 1) : link;
        return link;
    }

    protected void cleanAllIn() {
        allIn = new CopyOnWriteArraySet<>();
    }
}

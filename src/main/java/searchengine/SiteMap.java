package searchengine;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
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

    public SiteMap(String path) {
        this.path = path;
    }

    @Override
    protected StringBuilder compute() {
        StringBuilder siteMap = new StringBuilder(path + "\n");
        Set<SiteMap> listPath = new HashSet<>();
        getWay(listPath);
        for (SiteMap site : listPath) {
            try {
                siteMap.append(site.join());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return siteMap;
    }

    protected void getWay(Set<SiteMap> task) {
        if (getPool().isShutdown()) {
            return;
        }
        try {
            Document document = connectPath(path).get();
            Elements links = document.select("a[href]");
            for (Element link : links) {
                String cleanLink = link.attr("abs:href").trim().replace("www.", "").toLowerCase();
                cleanLink = (!(cleanLink.lastIndexOf("/") == (cleanLink.length() - 1)))
                        ? cleanLink + "/" : cleanLink;
                cleanLink = !(cleanLink.lastIndexOf("//") == cleanLink.indexOf("//"))
                        ? cleanLink.substring(0, cleanLink.lastIndexOf("//"))
                        + cleanLink.substring(cleanLink.lastIndexOf("//") + 1) : cleanLink;
                String siteUrl = path.replace(path.substring(path.indexOf('/', 16) + 1), "").replace("www.", "");
                String regex = siteUrl + "[A-Za-z0-9/\\-_]*(.html)?/?";
                if (!allIn.contains(cleanLink)
                        && !cleanLink.isEmpty()
                        && cleanLink.matches(regex)
                ) {
                    allIn.add(cleanLink);
                    SiteMap siteMap = new SiteMap(cleanLink);
                    task.add(siteMap);
                    siteMap.fork();
                }
            }
        } catch (IOException | InterruptedException | UncheckedIOException ex){
            logger.error("Ошибка парсинга страницы " + ex + " " + path);
        }
    }

    protected static Connection connectPath(String path) throws InterruptedException {
        Thread.sleep(5000);
        return Jsoup.connect(path)
                .userAgent("MollySearchBot")
                .referrer("http://www.google.com")
                .timeout(5000000)
                .maxBodySize(0)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true);
    }

    protected void cleanAllIn() {
        allIn = new CopyOnWriteArraySet<>();
    }
}

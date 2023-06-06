package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private IndexingService indexingService;
    @Autowired
    private StatisticsService statisticsService;
    @Autowired
    private SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() throws InterruptedException {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity indexPage(@RequestParam(name = "url", required = false, defaultValue = "") String url) {
        return ResponseEntity.ok(indexingService.indexPade(url.trim()));
    }

    @GetMapping("/search")
    public ResponseEntity search(
            @RequestParam(name = "query", defaultValue = "") String query,
            @RequestParam(name = "site", defaultValue = "") String siteUrl,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(searchService.search(query, siteUrl, offset, limit));
    }
}

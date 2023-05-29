package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.responses.FalseResponse;
import searchengine.dto.responses.Response;
import searchengine.dto.responses.TrueResponse;
import searchengine.services.SearchService;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<Response> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Response> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Response> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Response> indexPage(String url) {
        Response response = indexingService.indexPage(url);
        if (response instanceof TrueResponse) {
            return ResponseEntity.ok(response);
        } else {
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Response> search(@RequestParam @NonNull String query,
                                           @RequestParam(defaultValue = "0") int offset,
                                           @RequestParam(defaultValue = "20") int limit,
                                           @RequestParam @Nullable String site) {
        try {
            return new ResponseEntity<>(searchService.search(query, site, offset, limit),
                    HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new FalseResponse(false, "Указанная страница не найдена"),
                    HttpStatus.NOT_FOUND);
        }
    }
}

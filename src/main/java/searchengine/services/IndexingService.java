package searchengine.services;

import searchengine.dto.responses.Response;

public interface IndexingService {
    Response startIndexing();

    Response stopIndexing();

    Response indexPage(String url);
}

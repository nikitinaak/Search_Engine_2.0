package searchengine.services;

import searchengine.dto.responses.Response;

import java.io.IOException;

public interface SearchService {
    Response search(String query, String site, int offset, int limit) throws IOException;
}

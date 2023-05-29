package searchengine.dto.searching;

import lombok.Data;
import searchengine.dto.responses.Response;
import searchengine.model.SearchData;

import java.util.List;

@Data
public class SearchResponse implements Response {
    private boolean result;
    private int count;
    private List<SearchData> data;
}

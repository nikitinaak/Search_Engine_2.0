package searchengine.dto.searching;

import lombok.Data;
import org.springframework.data.domain.Page;
import searchengine.dto.responses.Response;
import searchengine.model.SearchData;

@Data
public class SearchResponse implements Response {
    private boolean result;
    private int count;
    private Page<SearchData> data;
}

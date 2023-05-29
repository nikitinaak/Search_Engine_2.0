package searchengine.dto.statistics;

import lombok.Data;
import searchengine.dto.responses.Response;

@Data
public class StatisticsResponse implements Response {
    private boolean result;
    private StatisticsData statistics;
}

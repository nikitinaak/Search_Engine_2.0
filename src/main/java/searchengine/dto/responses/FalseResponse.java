package searchengine.dto.responses;

public record FalseResponse(boolean result, String error) implements Response{
}

package searchengine.dto.index;

import lombok.Data;

@Data
public class IndexResponse {
    private boolean result;

    public IndexResponse(boolean result) {
        this.result = result;
    }
}

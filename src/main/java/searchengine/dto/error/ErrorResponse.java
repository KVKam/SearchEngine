package searchengine.dto.error;

import lombok.Data;

@Data
public class ErrorResponse {
    private boolean result;
    private String error;

    public ErrorResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}

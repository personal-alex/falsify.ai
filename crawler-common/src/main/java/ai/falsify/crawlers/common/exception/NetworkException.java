package ai.falsify.crawlers.common.exception;

/**
 * Exception thrown when network-related operations fail during crawling.
 * This includes connection failures, timeouts, and invalid responses.
 */
public class NetworkException extends CrawlingException {

    private final String url;
    private final int statusCode;

    public NetworkException(ErrorCode errorCode, String message, String url) {
        super(errorCode, message, url);
        this.url = url;
        this.statusCode = -1;
    }

    public NetworkException(ErrorCode errorCode, String message, String url, Throwable cause) {
        super(errorCode, message, url, cause);
        this.url = url;
        this.statusCode = -1;
    }

    public NetworkException(ErrorCode errorCode, String message, String url, int statusCode) {
        super(errorCode, message, url);
        this.url = url;
        this.statusCode = statusCode;
    }

    public NetworkException(ErrorCode errorCode, String message, String url, int statusCode, Throwable cause) {
        super(errorCode, message, url, cause);
        this.url = url;
        this.statusCode = statusCode;
    }

    public String getUrl() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean hasStatusCode() {
        return statusCode > 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NetworkException[")
          .append("code=").append(getErrorCode())
          .append(", url=").append(url);
        
        if (hasStatusCode()) {
            sb.append(", statusCode=").append(statusCode);
        }
        
        sb.append(", message=").append(getMessage())
          .append("]");
        
        return sb.toString();
    }

    // Convenience factory methods for common network errors
    public static NetworkException connectionFailed(String url, Throwable cause) {
        return new NetworkException(
            ErrorCode.NETWORK_CONNECTION_FAILED,
            "Failed to connect to: " + url,
            url,
            cause
        );
    }

    public static NetworkException timeout(String url, Throwable cause) {
        return new NetworkException(
            ErrorCode.NETWORK_TIMEOUT,
            "Request timed out for: " + url,
            url,
            cause
        );
    }

    public static NetworkException invalidResponse(String url, int statusCode) {
        return new NetworkException(
            ErrorCode.NETWORK_INVALID_RESPONSE,
            "Invalid response from: " + url + " (status: " + statusCode + ")",
            url,
            statusCode
        );
    }
}
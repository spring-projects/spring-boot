package org.springframework.boot.actuate.health;

import java.net.URI;
import java.time.Duration;

import org.springframework.http.client.HttpClient;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpResponse;

import lombok.Data;

/**
 * A health indicator that checks the status of an HTTP endpoint.
 *
 * @author Rohit Patidar
 * @since 2.0.0
 */
@Data
public class HttpHealthIndicator implements HealthIndicator {
    Logger logger = LoggerFactory.getLogger(HttpHealthIndicator.class);

    private final String url;
    private final int connectTimeout;
    private final int readTimeout;

    public HttpHealthIndicator(String url) {
        this(url, 5000, 5000);
    }

    public HttpHealthIndicator(String url, int connectTimeout, int readTimeout) {
        Assert.notNull(url, "Url must not be null");
        Assert.isTrue(connectTimeout >= 0, "Connect timeout must be non-negative");
        Assert.isTrue(readTimeout >= 0, "Read timeout must be non-negative");
        this.url = url;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    /**
     * Checks the status of the HTTP endpoint.
     *
     * @return a {@link Health} instance indicating the status of the endpoint
     */
    @Override
    public Health health() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().timeout(Duration.ofMillis(connectTimeout)).build();
            HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return Health.up().build();
            }
            return Health.down().withDetail("status", response.statusCode()).build();
        } 
        catch (Exception e) {
            return Health.down(e).build();
        }
    }
}

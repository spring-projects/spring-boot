package org.springframework.boot.actuate.health;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class HttpHealthIndicatorTests {
    
    @Mock
    private HttpClient client;

    private HttpHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new HttpHealthIndicator("http://example.com", 1000, 1000);
    }

    @Test
    void healthUp() throws Exception {
        HttpResponse<String> response = HttpResponse.of("OK", 200, null, null);
        when(client.send(HttpRequest.newBuilder().uri(URI.create("http://example.com")).GET().timeout(java.time.Duration.ofMillis(1000)).build(), HttpResponse.BodyHandlers.ofString()))
                .thenReturn(response);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void healthDown() throws Exception {
        HttpResponse<String> response = HttpResponse.of("Not Found", 404, null, null);
        when(client.send(HttpRequest.newBuilder().uri(URI.create("http://example.com")).GET().timeout(java.time.Duration.ofMillis(1000)).build(), HttpResponse.BodyHandlers.ofString()))
                .thenReturn(response);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("status")).isEqualTo(404);
    }

    @Test
    void healthDownException() throws Exception {
        when(client.send(HttpRequest.newBuilder().uri(URI.create("http://example.com")).GET().timeout(java.time.Duration.ofMillis(1000)).build(), HttpResponse.BodyHandlers.ofString()))
                .thenThrow(new RuntimeException());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("error")).isInstanceOf(RuntimeException.class);
    }
}

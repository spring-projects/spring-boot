package sample.actuator.customsecurity;

import java.net.URI;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for cors preflight requests to management endpoints.
 *
 * @author Madhura Bhave
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@ActiveProfiles("cors")
public class CorsSampleActuatorApplicationTests {

	private TestRestTemplate testRestTemplate;

	@Autowired
	private ApplicationContext applicationContext;

	@Before
	public void setUp() throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		LocalHostUriTemplateHandler handler = new LocalHostUriTemplateHandler(
				this.applicationContext.getEnvironment(), "http");
		restTemplate.setUriTemplateHandler(handler);
		restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
		this.testRestTemplate = new TestRestTemplate(restTemplate);
	}

	@Test
	public void endpointShouldReturnUnauthorized() throws Exception {
		ResponseEntity<?> entity = this.testRestTemplate.getForEntity("/application/env",
				Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void preflightRequestToEndpointShouldReturnOk() throws Exception {
		RequestEntity<?> healthRequest = RequestEntity
				.options(new URI("/application/env"))
				.header("Origin", "http://localhost:8080")
				.header("Access-Control-Request-Method", "GET").build();
		ResponseEntity<?> exchange = this.testRestTemplate.exchange(healthRequest,
				Map.class);
		assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void preflightRequestWhenCorsConfigInvalidShouldReturnForbidden()
			throws Exception {
		RequestEntity<?> entity = RequestEntity.options(new URI("/application/env"))
				.header("Origin", "http://localhost:9095")
				.header("Access-Control-Request-Method", "GET").build();
		ResponseEntity<byte[]> exchange = this.testRestTemplate.exchange(entity,
				byte[].class);
		assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

}

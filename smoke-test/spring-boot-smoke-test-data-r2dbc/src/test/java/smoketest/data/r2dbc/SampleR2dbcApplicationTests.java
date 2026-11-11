/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.r2dbc;

import javax.sql.DataSource;

import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.r2dbc.generate-unique-name=true")
@AutoConfigureWebTestClient
class SampleR2dbcApplicationTests {

	@Autowired
	private WebTestClient webClient;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void citiesEndpointReturnInitialState() {
		this.webClient.get()
			.uri("/cities")
			.exchange()
			.expectBody()
			.jsonPath("$[*].id")
			.isEqualTo(new JSONArray().appendElement(2000).appendElement(2001));
	}

	@Test
	void citiesEndpointByIdWithExistingIdReturnCity() {
		this.webClient.get().uri("/cities/2001").exchange().expectBody().jsonPath("$.name").isEqualTo("San Francisco");
	}

	@Test
	void healthEndpointHasR2dbcEntry() {
		this.webClient.get()
			.uri("/actuator/health")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("components.r2dbc.status")
			.isEqualTo("UP")
			.jsonPath("components.r2dbc.details.database")
			.isEqualTo("H2");
	}

	@Test
	void dataSourceIsNotAutoConfigured() {
		assertThat(this.applicationContext.getBeansOfType(DataSource.class)).isEmpty();
	}

}

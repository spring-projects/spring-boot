/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Tests for {@link RestClientObservationAutoConfiguration} without Micrometer Metrics.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
@ExtendWith(OutputCaptureExtension.class)
@ClassPathExclusions("micrometer-core-*.jar")
class RestClientObservationAutoConfigurationWithoutMetricsTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(ObservationRegistry.class, TestObservationRegistry::create)
		.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class, RestClientAutoConfiguration.class,
				RestClientObservationAutoConfiguration.class));

	@Test
	void restClientCreatedWithBuilderIsInstrumented() {
		this.contextRunner.run((context) -> {
			RestClient restClient = buildRestClient(context);
			restClient.get().uri("/projects/{project}", "spring-boot").retrieve().toBodilessEntity();
			TestObservationRegistry registry = context.getBean(TestObservationRegistry.class);
			assertThat(registry).hasObservationWithNameEqualToIgnoringCase("http.client.requests");
		});
	}

	private RestClient buildRestClient(AssertableApplicationContext context) {
		Builder builder = context.getBean(Builder.class);
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("/projects/spring-boot")).andRespond(withStatus(HttpStatus.OK));
		return builder.build();
	}

}

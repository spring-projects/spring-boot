/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.integrationtest;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.assertj.ApplicationContextAssertProvider;
import org.springframework.boot.test.context.runner.AbstractApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Abstract base class for health groups with an additional path.
 *
 * @param <T> the runner
 * @param <C> the application context type
 * @param <A> the assertions
 * @author Madhura Bhave
 */
abstract class AbstractHealthEndpointAdditionalPathIntegrationTests<T extends AbstractApplicationContextRunner<T, C, A>, C extends ConfigurableApplicationContext, A extends ApplicationContextAssertProvider<C>> {

	private final T runner;

	AbstractHealthEndpointAdditionalPathIntegrationTests(T runner) {
		this.runner = runner;
	}

	@Test
	void groupIsAvailableAtAdditionalPath() {
		this.runner
				.withPropertyValues("management.endpoint.health.group.live.include=diskSpace",
						"management.endpoint.health.group.live.additional-path=server:/healthz",
						"management.endpoint.health.group.live.show-components=always")
				.run(withWebTestClient(this::testResponse, "local.server.port"));
	}

	@Test
	void groupIsAvailableAtAdditionalPathWithoutSlash() {
		this.runner
				.withPropertyValues("management.endpoint.health.group.live.include=diskSpace",
						"management.endpoint.health.group.live.additional-path=server:healthz",
						"management.endpoint.health.group.live.show-components=always")
				.run(withWebTestClient(this::testResponse, "local.server.port"));
	}

	@Test
	void groupIsAvailableAtAdditionalPathOnManagementPort() {
		this.runner.withPropertyValues("management.endpoint.health.group.live.include=diskSpace",
				"management.server.port=0", "management.endpoint.health.group.live.additional-path=management:healthz",
				"management.endpoint.health.group.live.show-components=always")
				.run(withWebTestClient(this::testResponse, "local.management.port"));
	}

	@Test
	void groupIsAvailableAtAdditionalPathOnServerPortWithDifferentManagementPort() {
		this.runner.withPropertyValues("management.endpoint.health.group.live.include=diskSpace",
				"management.server.port=0", "management.endpoint.health.group.live.additional-path=server:healthz",
				"management.endpoint.health.group.live.show-components=always")
				.run(withWebTestClient(this::testResponse, "local.server.port"));
	}

	@Test
	void groupsAreNotConfiguredWhenHealthEndpointIsNotExposed() {
		this.runner
				.withPropertyValues("spring.jmx.enabled=true", "management.endpoints.web.exposure.exclude=health",
						"management.server.port=0", "management.endpoint.health.group.live.include=diskSpace",
						"management.endpoint.health.group.live.additional-path=server:healthz",
						"management.endpoint.health.group.live.show-components=always")
				.run(withWebTestClient((client) -> client.get().uri("/healthz").accept(MediaType.APPLICATION_JSON)
						.exchange().expectStatus().isNotFound(), "local.server.port"));
	}

	@Test
	void groupsAreNotConfiguredWhenHealthEndpointIsNotExposedAndCloudFoundryPlatform() {
		this.runner.withPropertyValues("spring.jmx.enabled=true", "management.endpoints.web.exposure.exclude=health",
				"spring.main.cloud-platform=cloud_foundry", "management.endpoint.health.group.live.include=diskSpace",
				"management.endpoint.health.group.live.additional-path=server:healthz",
				"management.endpoint.health.group.live.show-components=always")
				.run(withWebTestClient((client) -> client.get().uri("/healthz").accept(MediaType.APPLICATION_JSON)
						.exchange().expectStatus().isNotFound(), "local.server.port"));
	}

	@Test
	void groupsAreNotConfiguredWhenHealthEndpointIsNotExposedWithDifferentManagementPortAndCloudFoundryPlatform() {
		this.runner
				.withPropertyValues("spring.jmx.enabled=true", "management.endpoints.web.exposure.exclude=health",
						"spring.main.cloud-platform=cloud_foundry", "management.server.port=0",
						"management.endpoint.health.group.live.include=diskSpace",
						"management.endpoint.health.group.live.additional-path=server:healthz",
						"management.endpoint.health.group.live.show-components=always")
				.run(withWebTestClient((client) -> client.get().uri("/healthz").accept(MediaType.APPLICATION_JSON)
						.exchange().expectStatus().isNotFound(), "local.server.port"));
	}

	private void testResponse(WebTestClient client) {
		client.get().uri("/healthz").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectBody()
				.jsonPath("status").isEqualTo("UP").jsonPath("components.diskSpace").exists();
	}

	private ContextConsumer<A> withWebTestClient(Consumer<WebTestClient> consumer, String property) {
		return (context) -> {
			String port = context.getEnvironment().getProperty(property);
			WebTestClient client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
			consumer.accept(client);
		};
	}

}

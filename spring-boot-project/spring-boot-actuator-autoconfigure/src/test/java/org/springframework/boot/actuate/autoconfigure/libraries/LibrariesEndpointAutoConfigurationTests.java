/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.libraries;

import java.util.Collections;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.libraries.Libraries;
import org.springframework.boot.actuate.libraries.LibrariesContributor;
import org.springframework.boot.actuate.libraries.LibrariesEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link LibrariesEndpointAutoConfiguration}.
 *
 * @author Phil Clay
 */
class LibrariesEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LibrariesEndpointAutoConfiguration.class));

	@Test
	void runShouldHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=libraries")
				.run((context) -> assertThat(context).hasSingleBean(LibrariesEndpoint.class));
	}

	@Test
	void runWhenNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(LibrariesEndpoint.class));
	}

	@Test
	void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoint.libraries.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(LibrariesEndpoint.class));
	}

	@Test
	void defaultLibrariesContributorsDisabled() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=libraries",
				"management.endpoint.libraries.enabled=true", "management.libraries.defaults.enabled=false",
				"management.libraries.bundled.location=classpath:org/springframework/boot/actuate/autoconfigure/libraries/bundled-libraries.yaml")
				.run((context) -> assertThat(context).doesNotHaveBean(LibrariesContributor.class));
	}

	@Test
	void defaultLibrariesContributorsDisabledWithCustomOne() {
		this.contextRunner.withUserConfiguration(CustomLibrariesContributorConfiguration.class).withPropertyValues(
				"management.endpoints.web.exposure.include=libraries", "management.endpoint.libraries.enabled=true",
				"management.libraries.defaults.enabled=false",
				"management.libraries.bundled.location=classpath:org/springframework/boot/actuate/autoconfigure/libraries/bundled-libraries.yaml")
				.run((context) -> {
					Map<String, LibrariesContributor> beans = context.getBeansOfType(LibrariesContributor.class);
					assertThat(beans).hasSize(1);
					assertThat(context.getBean("customLibrariesContributor"))
							.isSameAs(beans.values().iterator().next());
				});
	}

	@Test
	void defaultLibrariesContributors() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=libraries",
				"management.endpoint.libraries.enabled=true",
				"management.libraries.bundled.location=classpath:org/springframework/boot/actuate/autoconfigure/libraries/bundled-libraries.yaml")
				.run((context) -> {
					Map<String, LibrariesContributor> beans = context.getBeansOfType(LibrariesContributor.class);
					assertThat(beans).hasSize(1);
					assertThat(context.getBean("bundledLibrariesContributor"))
							.isSameAs(beans.values().iterator().next());

					LibrariesEndpoint endpoint = context.getBean(LibrariesEndpoint.class);
					Libraries libraries = endpoint.libraries(Collections.emptyList());
					assertThat(libraries.get("bundled")).singleElement(as(InstanceOfAssertFactories.MAP))
							.containsExactly(entry("groupId", "my.group"), entry("artifactId", "my-artifact"),
									entry("version", "1.0.0"));
				});
	}

	@Test
	void bundledLibrariesDisabled() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=libraries",
				"management.endpoint.libraries.enabled=true", "management.libraries.bundled.enabled=false",
				"management.libraries.bundled.location=classpath:org/springframework/boot/actuate/autoconfigure/libraries/bundled-libraries.yaml")
				.run((context) -> {
					assertThat(context).doesNotHaveBean(LibrariesContributor.class);
					assertThat(context).hasSingleBean(LibrariesEndpoint.class);
				});
	}

	@Test
	void bundledLibrariesResourceNotFound() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=libraries",
				"management.endpoint.libraries.enabled=true").run((context) -> {
					assertThat(context).doesNotHaveBean(LibrariesContributor.class);
					assertThat(context).hasSingleBean(LibrariesEndpoint.class);
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomLibrariesContributorConfiguration {

		@Bean
		LibrariesContributor customLibrariesContributor() {
			return (builder) -> {
			};
		}

	}

}

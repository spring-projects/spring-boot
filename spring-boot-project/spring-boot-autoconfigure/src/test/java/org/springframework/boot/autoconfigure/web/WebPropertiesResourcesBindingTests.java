/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Binding tests for {@link WebProperties.Resources}.
 *
 * @author Stephane Nicoll
 */
class WebPropertiesResourcesBindingTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(TestConfiguration.class);

	@Test
	void staticLocationsExpandArray() {
		this.contextRunner
			.withPropertyValues("spring.web.resources.static-locations[0]=classpath:/one/",
					"spring.web.resources.static-locations[1]=classpath:/two",
					"spring.web.resources.static-locations[2]=classpath:/three/",
					"spring.web.resources.static-locations[3]=classpath:/four",
					"spring.web.resources.static-locations[4]=classpath:/five/",
					"spring.web.resources.static-locations[5]=classpath:/six")
			.run(assertResourceProperties((properties) -> assertThat(properties.getStaticLocations()).contains(
					"classpath:/one/", "classpath:/two/", "classpath:/three/", "classpath:/four/", "classpath:/five/",
					"classpath:/six/")));
	}

	private ContextConsumer<AssertableApplicationContext> assertResourceProperties(Consumer<Resources> consumer) {
		return (context) -> {
			assertThat(context).hasSingleBean(WebProperties.class);
			consumer.accept(context.getBean(WebProperties.class).getResources());
		};
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WebProperties.class)
	static class TestConfiguration {

	}

}

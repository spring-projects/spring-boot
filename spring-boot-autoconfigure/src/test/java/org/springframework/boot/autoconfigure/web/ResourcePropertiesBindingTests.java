/*
 * Copyright 2012-2018 the original author or authors.
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

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Binding tests for {@link ResourceProperties}.
 *
 * @author Stephane Nicoll
 */
public class ResourcePropertiesBindingTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void staticLocationsExpandArray() {
		ResourceProperties properties = load(
				"spring.resources.static-locations[0]=classpath:/one/",
				"spring.resources.static-locations[1]=classpath:/two",
				"spring.resources.static-locations[2]=classpath:/three/",
				"spring.resources.static-locations[3]=classpath:/four",
				"spring.resources.static-locations[4]=classpath:/five/",
				"spring.resources.static-locations[5]=classpath:/six");
		assertThat(properties.getStaticLocations()).contains("classpath:/one/",
				"classpath:/two/", "classpath:/three/", "classpath:/four/",
				"classpath:/five/", "classpath:/six/");
	}

	private ResourceProperties load(String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(ctx, environment);
		ctx.register(TestConfiguration.class);
		ctx.refresh();
		this.context = ctx;
		return this.context.getBean(ResourceProperties.class);
	}

	@Configuration
	@EnableConfigurationProperties(ResourceProperties.class)
	static class TestConfiguration {

	}

}

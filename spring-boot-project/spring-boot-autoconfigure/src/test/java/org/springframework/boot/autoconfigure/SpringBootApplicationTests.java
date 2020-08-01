/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootApplication @SpringBootApplication}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class SpringBootApplicationTests {

	@Test
	void proxyBeanMethodsIsEnabledByDefault() {
		AnnotationAttributes attributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(DefaultSpringBootApplication.class, Configuration.class);
		assertThat(attributes.get("proxyBeanMethods")).isEqualTo(true);
	}

	@Test
	void proxyBeanMethodsCanBeDisabled() {
		AnnotationAttributes attributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(NoBeanMethodProxyingSpringBootApplication.class, Configuration.class);
		assertThat(attributes.get("proxyBeanMethods")).isEqualTo(false);
	}

	@Test
	void nameGeneratorDefaultToBeanNameGenerator() {
		AnnotationAttributes attributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(DefaultSpringBootApplication.class, ComponentScan.class);
		assertThat(attributes.get("nameGenerator")).isEqualTo(BeanNameGenerator.class);
	}

	@Test
	void nameGeneratorCanBeSpecified() {
		AnnotationAttributes attributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(CustomNameGeneratorConfiguration.class, ComponentScan.class);
		assertThat(attributes.get("nameGenerator")).isEqualTo(TestBeanNameGenerator.class);
	}

	@SpringBootApplication
	static class DefaultSpringBootApplication {

	}

	@SpringBootApplication(proxyBeanMethods = false)
	static class NoBeanMethodProxyingSpringBootApplication {

	}

	@SpringBootApplication(nameGenerator = TestBeanNameGenerator.class)
	static class CustomNameGeneratorConfiguration {

	}

	static class TestBeanNameGenerator extends DefaultBeanNameGenerator {

	}

}

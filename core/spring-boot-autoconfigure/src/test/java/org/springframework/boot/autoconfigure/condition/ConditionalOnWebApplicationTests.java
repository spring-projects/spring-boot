/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.context.reactive.AnnotationConfigReactiveWebApplicationContext;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ConditionalOnWebApplication @ConditionalOnWebApplication}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class ConditionalOnWebApplicationTests {

	private @Nullable ConfigurableApplicationContext context;

	@AfterEach
	void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void testWebApplicationWithServletContext() {
		AnnotationConfigServletWebApplicationContext ctx = new AnnotationConfigServletWebApplicationContext();
		ctx.register(AnyWebApplicationConfiguration.class, ServletWebApplicationConfiguration.class,
				ReactiveWebApplicationConfiguration.class);
		ctx.setServletContext(new MockServletContext());
		ctx.refresh();
		this.context = ctx;
		assertThat(this.context.getBeansOfType(String.class)).containsExactly(entry("any", "any"),
				entry("servlet", "servlet"));
	}

	@Test
	void testWebApplicationWithReactiveContext() {
		AnnotationConfigReactiveWebApplicationContext context = new AnnotationConfigReactiveWebApplicationContext();
		context.register(AnyWebApplicationConfiguration.class, ServletWebApplicationConfiguration.class,
				ReactiveWebApplicationConfiguration.class);
		context.refresh();
		this.context = context;
		assertThat(this.context.getBeansOfType(String.class)).containsExactly(entry("any", "any"),
				entry("reactive", "reactive"));
	}

	@Test
	void testNonWebApplication() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AnyWebApplicationConfiguration.class, ServletWebApplicationConfiguration.class,
				ReactiveWebApplicationConfiguration.class);
		ctx.refresh();
		this.context = ctx;
		assertThat(this.context.getBeansOfType(String.class)).isEmpty();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication
	static class AnyWebApplicationConfiguration {

		@Bean
		String any() {
			return "any";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	static class ServletWebApplicationConfiguration {

		@Bean
		String servlet() {
			return "servlet";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.REACTIVE)
	static class ReactiveWebApplicationConfiguration {

		@Bean
		String reactive() {
			return "reactive";
		}

	}

}

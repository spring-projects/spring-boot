/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.condition;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.context.embedded.ReactiveWebApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ConditionalOnNotWebApplication}.
 *
 * @author Dave Syer$
 * @author Stephane Nicoll
 */
public class ConditionalOnNotWebApplicationTests {

	private ConfigurableApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testNotWebApplicationWithServletContext() {
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		ctx.register(NotWebApplicationConfiguration.class);
		ctx.setServletContext(new MockServletContext());
		ctx.refresh();

		this.context = ctx;
		assertThat(this.context.getBeansOfType(String.class)).isEmpty();
	}

	@Test
	public void testNotWebApplicationWithReactiveContext() {
		ReactiveWebApplicationContext ctx = new ReactiveWebApplicationContext();
		ctx.register(NotWebApplicationConfiguration.class);
		ctx.refresh();

		this.context = ctx;
		assertThat(this.context.getBeansOfType(String.class)).isEmpty();
	}

	@Test
	public void testNotWebApplication() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(NotWebApplicationConfiguration.class);
		ctx.refresh();

		this.context = ctx;
		assertThat(this.context.getBeansOfType(String.class)).containsExactly(
				entry("none", "none"));
	}

	@Configuration
	@ConditionalOnNotWebApplication
	protected static class NotWebApplicationConfiguration {

		@Bean
		public String none() {
			return "none";
		}

	}

}

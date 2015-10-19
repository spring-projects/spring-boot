/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.boot.redis.RedisTestServer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link SessionAutoConfiguration}.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
public class SessionAutoConfigurationTests {

	@Rule
	public RedisTestServer redis = new RedisTestServer();

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void flat() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Config.class, ServerPropertiesAutoConfiguration.class,
				RedisAutoConfiguration.class, SessionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertNotNull(server);
	}

	@Test
	public void hierarchy() throws Exception {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.register(RedisAutoConfiguration.class, SessionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		parent.refresh();
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.setParent(parent);
		this.context.register(Config.class, ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertNotNull(server);
	}

	@Configuration
	protected static class Config {

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			return new MockEmbeddedServletContainerFactory();
		}

	}

}

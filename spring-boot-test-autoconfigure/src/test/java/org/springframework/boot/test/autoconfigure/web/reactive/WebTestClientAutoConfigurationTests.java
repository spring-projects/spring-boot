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

package org.springframework.boot.test.autoconfigure.web.reactive;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link WebTestClientAutoConfiguration}
 *
 * @author Brian Clozel
 */
public class WebTestClientAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void shouldCustomizeClientCodecs() throws Exception {
		load(CodecConfiguration.class);
		WebTestClient webTestClient = this.context.getBean(WebTestClient.class);
		CodecCustomizer codecCustomizer = this.context.getBean(CodecCustomizer.class);
		assertThat(webTestClient).isNotNull();
		verify(codecCustomizer).customize(any(CodecConfigurer.class));
	}

	private void load(Class<?>... config) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(config);
		ctx.register(WebTestClientAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration
	static class BaseConfiguration {

		@Bean
		public WebHandler webHandler() {
			return mock(WebHandler.class);
		}
	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class CodecConfiguration {

		@Bean
		public CodecCustomizer myCodecCustomizer() {
			return mock(CodecCustomizer.class);
		}

	}

}

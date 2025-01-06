/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.io;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ProtocolResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProtocolResolverApplicationContextInitializer}.
 *
 * @author Scott Frederick
 */
class ProtocolResolverApplicationContextInitializerTests {

	@Test
	void initializeAddsProtocolResolversToApplicationContext() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext()) {
			ProtocolResolverApplicationContextInitializer initializer = new ProtocolResolverApplicationContextInitializer();
			initializer.initialize(context);
			assertThat(context).isInstanceOf(DefaultResourceLoader.class);
			Collection<ProtocolResolver> protocolResolvers = ((DefaultResourceLoader) context).getProtocolResolvers();
			assertThat(protocolResolvers).hasExactlyElementsOfTypes(Base64ProtocolResolver.class);
		}
	}

}

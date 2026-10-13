/*
 * Copyright 2012-present the original author or authors.
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

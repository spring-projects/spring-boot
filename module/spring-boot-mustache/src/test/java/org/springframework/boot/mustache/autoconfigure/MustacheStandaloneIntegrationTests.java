/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mustache.autoconfigure;

import java.util.Collections;

import com.samskivert.mustache.Mustache;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for {@link MustacheAutoConfiguration} outside of a web application.
 *
 * @author Dave Syer
 */
@DirtiesContext
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class MustacheStandaloneIntegrationTests {

	@Autowired
	private Mustache.Compiler compiler;

	@Test
	void directCompilation() {
		assertThat(this.compiler.compile("Hello: {{world}}").execute(Collections.singletonMap("world", "World")))
			.isEqualTo("Hello: World");
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ MustacheAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	static class Application {

	}

}

/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mustache.autoconfigure;

import com.samskivert.mustache.Mustache;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MustacheAutoConfiguration} without Spring MVC on the class path.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("spring-webmvc-*.jar")
class MustacheAutoConfigurationWithoutWebMvcTests {

	@Test
	void registerBeansForServletAppWithoutMvc() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(MustacheAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(Mustache.Compiler.class);
				assertThat(context).hasSingleBean(MustacheResourceTemplateLoader.class);
			});
	}

}

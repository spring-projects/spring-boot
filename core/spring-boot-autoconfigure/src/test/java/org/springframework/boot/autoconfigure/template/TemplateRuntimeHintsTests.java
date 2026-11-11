/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.template;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TemplateRuntimeHints}.
 *
 * @author Stephane Nicoll
 */
class TemplateRuntimeHintsTests {

	private static final Predicate<RuntimeHints> TEST_PREDICATE = RuntimeHintsPredicates.resource()
		.forResource("templates/something/hello.html");

	@Test
	void templateRuntimeHintsIsRegistered() {
		Iterable<RuntimeHintsRegistrar> registrar = AotServices.factories().load(RuntimeHintsRegistrar.class);
		assertThat(registrar).anyMatch(TemplateRuntimeHints.class::isInstance);
	}

	@Test
	@WithResource(name = "templates/test.html")
	void contributeWhenTemplateLocationExists() {
		RuntimeHints runtimeHints = contribute(Thread.currentThread().getContextClassLoader());
		assertThat(TEST_PREDICATE.test(runtimeHints)).isTrue();
	}

	@Test
	void contributeWhenTemplateLocationDoesNotExist() {
		FilteredClassLoader classLoader = new FilteredClassLoader(new ClassPathResource("templates"));
		RuntimeHints runtimeHints = contribute(classLoader);
		assertThat(TEST_PREDICATE.test(runtimeHints)).isFalse();
	}

	private RuntimeHints contribute(ClassLoader classLoader) {
		RuntimeHints runtimeHints = new RuntimeHints();
		new TemplateRuntimeHints().registerHints(runtimeHints, classLoader);
		return runtimeHints;
	}

}

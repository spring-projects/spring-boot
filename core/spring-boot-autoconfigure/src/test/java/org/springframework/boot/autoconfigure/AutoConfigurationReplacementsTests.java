/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigurationReplacements}.
 *
 * @author Phillip Webb
 */
@WithResource(
		name = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfigurationReplacementsTests$TestAutoConfigurationReplacements.replacements",
		content = """
				com.example.A1=com.example.A2
				com.example.B1=com.example.B2
				""")
class AutoConfigurationReplacementsTests {

	private AutoConfigurationReplacements replacements;

	@BeforeEach
	void loadReplacements() {
		this.replacements = AutoConfigurationReplacements.load(TestAutoConfigurationReplacements.class,
				Thread.currentThread().getContextClassLoader());
	}

	@Test
	void replaceWhenMatchReplacesClassName() {
		assertThat(this.replacements.replace("com.example.A1")).isEqualTo("com.example.A2");
	}

	@Test
	void replaceWhenNoMatchReturnsOriginalClassName() {
		assertThat(this.replacements.replace("com.example.Z1")).isEqualTo("com.example.Z1");
	}

	@Test
	void replaceAllReplacesAllMatching() {
		Set<String> classNames = new LinkedHashSet<>(
				List.of("com.example.A1", "com.example.B1", "com.example.Y1", "com.example.Z1"));
		assertThat(this.replacements.replaceAll(classNames)).containsExactly("com.example.A2", "com.example.B2",
				"com.example.Y1", "com.example.Z1");
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface TestAutoConfigurationReplacements {

	}

}

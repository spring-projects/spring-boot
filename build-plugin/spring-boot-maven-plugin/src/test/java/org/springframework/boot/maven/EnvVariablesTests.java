/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.maven;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link EnvVariables}.
 *
 * @author Dmytro Nosan
 */
class EnvVariablesTests {

	@Test
	void asNull() {
		Map<String, String> args = new EnvVariables(null).asMap();
		assertThat(args).isEmpty();
	}

	@Test
	void asArray() {
		assertThat(new EnvVariables(getTestArgs()).asArray()).contains("key=My Value", "key1= tt ", "key2=   ",
				"key3=");
	}

	@Test
	void asMap() {
		assertThat(new EnvVariables(getTestArgs()).asMap()).containsExactly(entry("key", "My Value"),
				entry("key1", " tt "), entry("key2", "   "), entry("key3", ""));
	}

	private Map<String, String> getTestArgs() {
		Map<String, String> args = new LinkedHashMap<>();
		args.put("key", "My Value");
		args.put("key1", " tt ");
		args.put("key2", "   ");
		args.put("key3", null);
		return args;
	}

}

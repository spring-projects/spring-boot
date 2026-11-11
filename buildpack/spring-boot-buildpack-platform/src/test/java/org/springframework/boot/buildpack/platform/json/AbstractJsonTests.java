/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for JSON based tests.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
public abstract class AbstractJsonTests {

	protected final JsonMapper getJsonMapper() {
		return SharedJsonMapper.get();
	}

	protected final InputStream getContent(String name) {
		InputStream result = getClass().getResourceAsStream(name);
		assertThat(result).as("JSON source " + name).isNotNull();
		return result;
	}

	protected final String getContentAsString(String name) {
		try (InputStream in = getContent(name)) {
			return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.joining("\n"));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}

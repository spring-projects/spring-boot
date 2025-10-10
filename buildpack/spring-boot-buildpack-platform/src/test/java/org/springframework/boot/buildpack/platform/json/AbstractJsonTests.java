/*
 * Copyright 2012-present the original author or authors.
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

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

package org.springframework.boot.env;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

/**
 * Strategy to load '.env' files into a {@link PropertySource}.
 * <p>
 * A '.env' file contains environment variables, one per line, in the form
 * {@code [export ]NAME=value}. Blank lines and lines that start with {@code #} are
 * ignored, a {@code #} that follows whitespace starts a comment at the end of an unquoted
 * value, and a value can be quoted with single or double quotes, which is required for a
 * value that spans multiple lines or has leading or trailing whitespace. Double quoted
 * values support the {@code \n}, {@code \r}, {@code \t}, {@code \"} and {@code \\} escape
 * sequences, whereas single quoted values are taken literally. See the reference
 * documentation for details.
 * <p>
 * The variables are exposed by a
 * {@link org.springframework.core.env.SystemEnvironmentPropertySource
 * SystemEnvironmentPropertySource}, so they can be accessed and bound using the same
 * relaxed names as real environment variables, for example {@code SPRING_DATASOURCE_URL}
 * can be accessed and bound as {@code spring.datasource.url}.
 *
 * @author Sharang Gupta
 * @since 4.2.0
 */
public class DotEnvPropertySourceLoader implements PropertySourceLoader {

	@Override
	public String[] getFileExtensions() {
		return new String[] { "env" };
	}

	@Override
	public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
		return load(name, resource, null);
	}

	@Override
	public List<PropertySource<?>> load(String name, Resource resource, @Nullable Charset encoding) throws IOException {
		Map<String, Object> variables = OriginTrackedDotEnvLoader.load(resource, encoding);
		if (variables.isEmpty()) {
			return Collections.emptyList();
		}
		return List.of(new DotEnvPropertySource(name, Collections.unmodifiableMap(variables)));
	}

}

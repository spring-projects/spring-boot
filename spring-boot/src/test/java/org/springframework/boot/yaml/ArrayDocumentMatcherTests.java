/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.yaml;

import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import org.springframework.beans.factory.config.YamlProcessor.MatchStatus;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ArrayDocumentMatcher}.
 *
 * @author Dave Syer
 */
public class ArrayDocumentMatcherTests {

	@Test
	public void testMatchesSingleValue() throws IOException {
		ArrayDocumentMatcher matcher = new ArrayDocumentMatcher("foo", "bar");
		assertThat(matcher.matches(getProperties("foo: bar")))
				.isEqualTo(MatchStatus.FOUND);
	}

	@Test
	public void testDoesNotMatchesIndexedArray() throws IOException {
		ArrayDocumentMatcher matcher = new ArrayDocumentMatcher("foo", "bar");
		assertThat(matcher.matches(getProperties("foo[0]: bar\nfoo[1]: spam")))
				.isEqualTo(MatchStatus.ABSTAIN);
	}

	@Test
	public void testMatchesCommaSeparatedArray() throws IOException {
		ArrayDocumentMatcher matcher = new ArrayDocumentMatcher("foo", "bar");
		assertThat(matcher.matches(getProperties("foo: bar,spam")))
				.isEqualTo(MatchStatus.FOUND);
	}

	private Properties getProperties(String values) throws IOException {
		return PropertiesLoaderUtils
				.loadProperties(new ByteArrayResource(values.getBytes()));
	}

}

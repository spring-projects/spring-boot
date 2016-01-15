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

import org.junit.Assert;
import org.junit.Test;

import org.springframework.beans.factory.config.YamlProcessor.MatchStatus;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * Tests for {@link SpringProfileDocumentMatcher}.
 *
 * @author Matt Benson
 */
public class SpringProfileDocumentMatcherTests {

	@Test
	public void testMatchesSingleProfile() throws IOException {
		SpringProfileDocumentMatcher matcher = new SpringProfileDocumentMatcher("foo",
				"bar");
		Assert.assertSame(MatchStatus.FOUND,
				matcher.matches(getProperties("spring.profiles: foo")));
	}

	@Test
	public void testAbstainNoConfiguredProfiles() throws IOException {
		SpringProfileDocumentMatcher matcher = new SpringProfileDocumentMatcher("foo",
				"bar");
		Assert.assertSame(MatchStatus.ABSTAIN,
				matcher.matches(getProperties("some.property: spam")));
	}

	@Test
	public void testNoActiveProfiles() throws IOException {
		SpringProfileDocumentMatcher matcher = new SpringProfileDocumentMatcher();
		Assert.assertSame(MatchStatus.NOT_FOUND,
				matcher.matches(getProperties("spring.profiles: bar,spam")));
	}

	@Test
	public void testMatchesCommaSeparatedArray() throws IOException {
		SpringProfileDocumentMatcher matcher = new SpringProfileDocumentMatcher("foo",
				"bar");
		Assert.assertSame(MatchStatus.FOUND,
				matcher.matches(getProperties("spring.profiles: bar,spam")));
	}

	@Test
	public void testNoMatchingProfiles() throws IOException {
		SpringProfileDocumentMatcher matcher = new SpringProfileDocumentMatcher("foo",
				"bar");
		Assert.assertSame(MatchStatus.NOT_FOUND,
				matcher.matches(getProperties("spring.profiles: baz,blah")));
	}

	@Test
	public void testInverseMatchSingle() throws IOException {
		SpringProfileDocumentMatcher matcher = new SpringProfileDocumentMatcher("foo",
				"bar");
		Assert.assertSame(MatchStatus.FOUND,
				matcher.matches(getProperties("spring.profiles: !baz")));
	}

	@Test
	public void testInverseMatchMulti() throws IOException {
		SpringProfileDocumentMatcher matcher = new SpringProfileDocumentMatcher("foo",
				"bar");
		Assert.assertSame(MatchStatus.FOUND,
				matcher.matches(getProperties("spring.profiles: !baz,!blah")));
	}

	@Test
	public void testNegatedAndNonNegated() throws IOException {
		SpringProfileDocumentMatcher matcher = new SpringProfileDocumentMatcher("foo",
				"bar", "blah");
		Assert.assertSame(MatchStatus.FOUND,
				matcher.matches(getProperties("spring.profiles: !baz,blah")));
	}

	@Test
	public void testNegatedTrumpsMatching() throws IOException {
		SpringProfileDocumentMatcher matcher = new SpringProfileDocumentMatcher("foo",
				"baz", "blah");
		Assert.assertSame(MatchStatus.NOT_FOUND,
				matcher.matches(getProperties("spring.profiles: !baz,blah")));
	}

	private Properties getProperties(String values) throws IOException {
		return PropertiesLoaderUtils
				.loadProperties(new ByteArrayResource(values.getBytes()));
	}

}

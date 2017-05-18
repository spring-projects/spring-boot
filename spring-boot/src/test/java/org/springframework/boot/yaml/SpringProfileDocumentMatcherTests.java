/*
 * Copyright 2012-2017 the original author or authors.
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

import org.springframework.beans.factory.config.YamlProcessor.DocumentMatcher;
import org.springframework.beans.factory.config.YamlProcessor.MatchStatus;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringProfileDocumentMatcher}.
 *
 * @author Matt Benson
 * @author Andy Wilkinson
 */
public class SpringProfileDocumentMatcherTests {

	@Test
	public void matchesSingleProfile() throws IOException {
		DocumentMatcher matcher = new SpringProfileDocumentMatcher("foo", "bar");
		Properties properties = getProperties("spring.ProfILEs: foo");
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.FOUND);
	}

	@Test
	public void abstainNoConfiguredProfiles() throws IOException {
		DocumentMatcher matcher = new SpringProfileDocumentMatcher("foo", "bar");
		Properties properties = getProperties("some.property: spam");
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.ABSTAIN);
	}

	@Test
	public void noActiveProfiles() throws IOException {
		DocumentMatcher matcher = new SpringProfileDocumentMatcher();
		Properties properties = getProperties("spring.profiles: bar,spam");
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.NOT_FOUND);
	}

	@Test
	public void matchesCommaSeparatedString() throws IOException {
		DocumentMatcher matcher = new SpringProfileDocumentMatcher("foo", "bar");
		Properties properties = getProperties("spring.profiles: bar,spam");
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.FOUND);
	}

	@Test
	public void matchesCommaSeparatedArray() throws IOException {
		DocumentMatcher matcher = new SpringProfileDocumentMatcher("foo", "bar");
		Properties properties = getProperties("spring.profiles: [bar, spam]");
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.FOUND);
	}

	@Test
	public void matchesList() throws IOException {
		DocumentMatcher matcher = new SpringProfileDocumentMatcher("foo", "bar");
		Properties properties = getProperties(
				String.format("spring.profiles:%n  - bar%n  - spam"));
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.FOUND);
	}

	@Test
	public void noMatchingProfiles() throws IOException {
		DocumentMatcher matcher = new SpringProfileDocumentMatcher("foo", "bar");
		Properties properties = getProperties("spring.profiles: baz,blah");
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.NOT_FOUND);
	}

	@Test
	public void inverseMatchSingle() throws IOException {
		DocumentMatcher matcher = new SpringProfileDocumentMatcher("foo", "bar");
		Properties properties = getProperties("spring.profiles: '!baz'");
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.FOUND);
	}

	@Test
	public void testInverseMatchMulti() throws IOException {
		DocumentMatcher matcher = new SpringProfileDocumentMatcher("foo", "bar");
		Properties properties = getProperties("spring.profiles: '!baz,!blah'");
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.FOUND);
	}

	@Test
	public void negatedWithMatch() throws Exception {
		DocumentMatcher matcher = new SpringProfileDocumentMatcher("foo", "bar", "blah");
		Properties properties = getProperties("spring.profiles: '!baz,blah'");
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.FOUND);
	}

	@Test
	public void negatedWithNoMatch() throws IOException {
		DocumentMatcher matcher = new SpringProfileDocumentMatcher("foo", "bar", "blah");
		Properties properties = getProperties("spring.profiles: '!baz,another'");
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.NOT_FOUND);
	}

	@Test
	public void negatedTrumpsMatching() throws IOException {
		DocumentMatcher matcher = new SpringProfileDocumentMatcher("foo", "baz", "blah");
		Properties properties = getProperties("spring.profiles: '!baz,blah'");
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.NOT_FOUND);
	}

	private Properties getProperties(String values) throws IOException {
		YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
		ByteArrayResource resource = new ByteArrayResource(values.getBytes());
		yamlPropertiesFactoryBean.setResources(resource);
		yamlPropertiesFactoryBean.afterPropertiesSet();
		return yamlPropertiesFactoryBean.getObject();
	}

}

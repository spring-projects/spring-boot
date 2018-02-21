/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.env;

import java.util.Properties;

import org.junit.Test;

import org.springframework.beans.factory.config.YamlProcessor.MatchStatus;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringProfilesDocumentMatcher}.
 *
 * @author Phillip Webb
 */
public class SpringProfilesDocumentMatcherTests {

	private TestSpringProfilesDocumentMatcher matcher = new TestSpringProfilesDocumentMatcher();

	@Test
	public void matchesShouldBindAgainstCommaList() {
		Properties properties = new Properties();
		properties.put("spring.profiles", "foo,bar");
		this.matcher.matches(properties);
		assertThat(this.matcher.getProfiles()).containsExactly("foo", "bar");
	}

	@Test
	public void matchesShouldBindAgainstYamlList() {
		Properties properties = new Properties();
		properties.put("spring.profiles[0]", "foo");
		properties.put("spring.profiles[1]", "bar");
		this.matcher.matches(properties);
		assertThat(this.matcher.getProfiles()).containsExactly("foo", "bar");
	}

	@Test
	public void matchesShouldBindAgainstOriginTrackedValue() {
		Properties properties = new Properties();
		properties.put("spring.profiles", OriginTrackedValue.of("foo,bar"));
		this.matcher.matches(properties);
		assertThat(this.matcher.getProfiles()).containsExactly("foo", "bar");
	}

	@Test
	public void matchesWhenMatchShouldReturnAbstain() {
		Properties properties = new Properties();
		properties.put("spring.profiles", "foo,bar");
		assertThat(this.matcher.matches(properties)).isEqualTo(MatchStatus.ABSTAIN);
	}

	@Test
	public void matchesWhenNoMatchShouldReturnNotFound() {
		Properties properties = new Properties();
		assertThat(this.matcher.matches(properties)).isEqualTo(MatchStatus.NOT_FOUND);
	}

	private static class TestSpringProfilesDocumentMatcher
			extends SpringProfilesDocumentMatcher {

		private String[] profiles;

		@Override
		protected boolean matches(String[] profiles) {
			this.profiles = profiles;
			return !ObjectUtils.isEmpty(profiles);
		}

		public String[] getProfiles() {
			return this.profiles;
		}

	}

}

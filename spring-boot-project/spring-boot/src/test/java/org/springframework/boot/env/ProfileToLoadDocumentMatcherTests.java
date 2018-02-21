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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProfileToLoadDocumentMatcher}.
 *
 * @author Phillip Webb
 */
public class ProfileToLoadDocumentMatcherTests {

	@Test
	public void matchesWhenProfilesIsNullAndHasNoProfilePropertiesShouldReturnAbstain() {
		ProfileToLoadDocumentMatcher matcher = new ProfileToLoadDocumentMatcher(null);
		Properties properties = new Properties();
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.ABSTAIN);
	}

	@Test
	public void matchesWhenProfileIsNullAndHasOnlyNegativeProfilePropertiesShouldReturnAbstain() {
		ProfileToLoadDocumentMatcher matcher = new ProfileToLoadDocumentMatcher(null);
		Properties properties = new Properties();
		properties.put("spring.profiles", "!foo,!bar");
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.ABSTAIN);
	}

	@Test
	public void matchesWhenProfileIsNullAndHasProfilePropertyShouldReturnNotFound() {
		ProfileToLoadDocumentMatcher matcher = new ProfileToLoadDocumentMatcher(null);
		Properties properties = new Properties();
		properties.put("spring.profiles", "!foo,!bar,baz");
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.NOT_FOUND);
	}

	@Test
	public void matchesWhenProfilesIsSetAndHasNoProfilePropertiesShouldReturnNotFound() {
		ProfileToLoadDocumentMatcher matcher = new ProfileToLoadDocumentMatcher("bar");
		Properties properties = new Properties();
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.NOT_FOUND);
	}

	@Test
	public void matchesWhenProfileIsSetAndHasOnlyNegativeProfilePropertiesShouldReturnNotFound() {
		ProfileToLoadDocumentMatcher matcher = new ProfileToLoadDocumentMatcher("bar");
		Properties properties = new Properties();
		properties.put("spring.profiles", "!foo,!bar,baz");
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.NOT_FOUND);
	}

	@Test
	public void matchesWhenProfileIsSetAndHasProfilePropertyShouldReturnAbstain() {
		ProfileToLoadDocumentMatcher matcher = new ProfileToLoadDocumentMatcher("bar");
		Properties properties = new Properties();
		properties.put("spring.profiles", "!foo,bar,baz");
		assertThat(matcher.matches(properties)).isEqualTo(MatchStatus.ABSTAIN);
	}

}

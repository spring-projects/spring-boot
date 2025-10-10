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

package org.springframework.boot.info;

import java.util.Properties;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GitProperties}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 */
class GitPropertiesTests {

	@Test
	void basicInfo() {
		GitProperties properties = new GitProperties(
				createProperties("master", "abcdefghijklmno", "abcdefg", "1457527123"));
		assertThat(properties.getBranch()).isEqualTo("master");
		assertThat(properties.getCommitId()).isEqualTo("abcdefghijklmno");
		assertThat(properties.getShortCommitId()).isEqualTo("abcdefg");
	}

	@Test
	void noInfo() {
		GitProperties properties = new GitProperties(new Properties());
		assertThat(properties.getBranch()).isNull();
		assertThat(properties.getCommitId()).isNull();
		assertThat(properties.getShortCommitId()).isNull();
		assertThat(properties.getCommitTime()).isNull();
	}

	@Test
	void coerceEpochSecond() {
		GitProperties properties = new GitProperties(createProperties("master", "abcdefg", null, "1457527123"));
		assertThat(properties.getCommitTime()).isNotNull();
		assertThat(properties.get("commit.time")).isEqualTo("1457527123000");
		assertThat(properties.getCommitTime().toEpochMilli()).isEqualTo(1457527123000L);
	}

	@Test
	void coerceLegacyDateString() {
		GitProperties properties = new GitProperties(
				createProperties("master", "abcdefg", null, "2016-03-04T14:36:33+0100"));
		assertThat(properties.getCommitTime()).isNotNull();
		assertThat(properties.get("commit.time")).isEqualTo("1457098593000");
		assertThat(properties.getCommitTime().toEpochMilli()).isEqualTo(1457098593000L);
	}

	@Test
	void coerceDateString() {
		GitProperties properties = new GitProperties(
				createProperties("master", "abcdefg", null, "2016-03-04T14:36:33+01:00"));
		assertThat(properties.getCommitTime()).isNotNull();
		assertThat(properties.get("commit.time")).isEqualTo("1457098593000");
		assertThat(properties.getCommitTime().toEpochMilli()).isEqualTo(1457098593000L);
	}

	@Test
	void coerceUnsupportedFormat() {
		GitProperties properties = new GitProperties(
				createProperties("master", "abcdefg", null, "2016-03-04 15:22:24"));
		assertThat(properties.getCommitTime()).isNull();
		assertThat(properties.get("commit.time")).isEqualTo("2016-03-04 15:22:24");
	}

	@Test
	void shortCommitUsedIfPresent() {
		GitProperties properties = new GitProperties(
				createProperties("master", "abcdefghijklmno", "abcdefgh", "1457527123"));
		assertThat(properties.getCommitId()).isEqualTo("abcdefghijklmno");
		assertThat(properties.getShortCommitId()).isEqualTo("abcdefgh");
	}

	@Test
	void shortenCommitIdShorterThan7() {
		GitProperties properties = new GitProperties(createProperties("master", "abc", null, "1457527123"));
		assertThat(properties.getCommitId()).isEqualTo("abc");
		assertThat(properties.getShortCommitId()).isEqualTo("abc");
	}

	@Test
	void shortenCommitIdLongerThan7() {
		GitProperties properties = new GitProperties(createProperties("master", "abcdefghijklmno", null, "1457527123"));
		assertThat(properties.getCommitId()).isEqualTo("abcdefghijklmno");
		assertThat(properties.getShortCommitId()).isEqualTo("abcdefg");
	}

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new GitProperties.GitPropertiesRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.resource().forResource("git.properties")).accepts(runtimeHints);
	}

	private static Properties createProperties(String branch, String commitId, @Nullable String commitIdAbbrev,
			String commitTime) {
		Properties properties = new Properties();
		properties.put("branch", branch);
		properties.put("commit.id", commitId);
		if (commitIdAbbrev != null) {
			properties.put("commit.id.abbrev", commitIdAbbrev);
		}
		properties.put("commit.time", commitTime);
		return properties;
	}

}

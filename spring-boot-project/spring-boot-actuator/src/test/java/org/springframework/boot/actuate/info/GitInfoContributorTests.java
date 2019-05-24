/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.info;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.info.InfoPropertiesInfoContributor.Mode;
import org.springframework.boot.info.GitProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GitInfoContributor}.
 *
 * @author Stephane Nicoll
 */
class GitInfoContributorTests {

	@Test
	@SuppressWarnings("unchecked")
	void coerceDate() {
		Properties properties = new Properties();
		properties.put("branch", "master");
		properties.put("commit.time", "2016-03-04T14:36:33+0100");
		GitInfoContributor contributor = new GitInfoContributor(new GitProperties(properties));
		Map<String, Object> content = contributor.generateContent();
		assertThat(content.get("commit")).isInstanceOf(Map.class);
		Map<String, Object> commit = (Map<String, Object>) content.get("commit");
		Object commitTime = commit.get("time");
		assertThat(commitTime).isInstanceOf(Instant.class);
		assertThat(((Instant) commitTime).toEpochMilli()).isEqualTo(1457098593000L);
	}

	@Test
	@SuppressWarnings("unchecked")
	void shortenCommitId() {
		Properties properties = new Properties();
		properties.put("branch", "master");
		properties.put("commit.id", "8e29a0b0d423d2665c6ee5171947c101a5c15681");
		GitInfoContributor contributor = new GitInfoContributor(new GitProperties(properties));
		Map<String, Object> content = contributor.generateContent();
		assertThat(content.get("commit")).isInstanceOf(Map.class);
		Map<String, Object> commit = (Map<String, Object>) content.get("commit");
		assertThat(commit.get("id")).isEqualTo("8e29a0b");
	}

	@Test
	@SuppressWarnings("unchecked")
	void withGitIdAndAbbrev() {
		// gh-11892
		Properties properties = new Properties();
		properties.put("branch", "master");
		properties.put("commit.id", "1b3cec34f7ca0a021244452f2cae07a80497a7c7");
		properties.put("commit.id.abbrev", "1b3cec3");
		GitInfoContributor contributor = new GitInfoContributor(new GitProperties(properties), Mode.FULL);
		Map<String, Object> content = contributor.generateContent();
		Map<String, Object> commit = (Map<String, Object>) content.get("commit");
		assertThat(commit.get("id")).isInstanceOf(Map.class);
		Map<String, Object> id = (Map<String, Object>) commit.get("id");
		assertThat(id.get("full")).isEqualTo("1b3cec34f7ca0a021244452f2cae07a80497a7c7");
		assertThat(id.get("abbrev")).isEqualTo("1b3cec3");
	}

}

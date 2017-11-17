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

package org.springframework.boot.actuate.info;

import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import org.springframework.boot.info.GitProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GitInfoContributor}.
 *
 * @author Stephane Nicoll
 */
public class GitInfoContributorTests {

	@SuppressWarnings("unchecked")
	@Test
	public void coerceDate() {
		Properties properties = new Properties();
		properties.put("branch", "master");
		properties.put("commit.time", "2016-03-04T14:36:33+0100");
		GitInfoContributor contributor = new GitInfoContributor(
				new GitProperties(properties));
		Map<String, Object> content = contributor.generateContent();
		assertThat(content.get("commit")).isInstanceOf(Map.class);
		Map<String, Object> commit = (Map<String, Object>) content.get("commit");
		Object commitTime = commit.get("time");
		assertThat(commitTime).isInstanceOf(Date.class);
		assertThat(((Date) commitTime).getTime()).isEqualTo(1457098593000L);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shortenCommitId() {
		Properties properties = new Properties();
		properties.put("branch", "master");
		properties.put("commit.id", "8e29a0b0d423d2665c6ee5171947c101a5c15681");
		GitInfoContributor contributor = new GitInfoContributor(
				new GitProperties(properties));
		Map<String, Object> content = contributor.generateContent();
		assertThat(content.get("commit")).isInstanceOf(Map.class);
		Map<String, Object> commit = (Map<String, Object>) content.get("commit");
		assertThat(commit.get("id")).isEqualTo("8e29a0b");
	}

}

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

package org.springframework.boot.info;

import java.util.Properties;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GitProperties}.
 *
 * @author Stephane Nicoll
 */
public class GitPropertiesTests {

	@Test
	public void basicInfo() {
		GitProperties properties = new GitProperties(
				createProperties("master", "abcdefghijklmno", "1457527123"));
		assertThat(properties.getBranch()).isEqualTo("master");
		assertThat(properties.getCommitId()).isEqualTo("abcdefghijklmno");
		assertThat(properties.getShortCommitId()).isEqualTo("abcdefg");
	}

	@Test
	public void noInfo() {
		GitProperties properties = new GitProperties(new Properties());
		assertThat(properties.getBranch()).isNull();
		assertThat(properties.getCommitId()).isNull();
		assertThat(properties.getShortCommitId()).isNull();
		assertThat(properties.getCommitTime()).isNull();
	}

	@Test
	public void coerceEpochSecond() {
		GitProperties properties = new GitProperties(
				createProperties("master", "abcdefg", "1457527123"));
		assertThat(properties.getCommitTime()).isNotNull();
		assertThat(properties.get("commit.time")).isEqualTo("1457527123000");
		assertThat(properties.getCommitTime().getTime()).isEqualTo(1457527123000L);
	}

	@Test
	public void coerceDateString() {
		GitProperties properties = new GitProperties(
				createProperties("master", "abcdefg", "2016-03-04T14:36:33+0100"));
		assertThat(properties.getCommitTime()).isNotNull();
		assertThat(properties.get("commit.time")).isEqualTo("1457098593000");
		assertThat(properties.getCommitTime().getTime()).isEqualTo(1457098593000L);
	}

	@Test
	public void coerceUnsupportedFormat() {
		GitProperties properties = new GitProperties(
				createProperties("master", "abcdefg", "2016-03-04 15:22:24"));
		assertThat(properties.getCommitTime()).isNull();
		assertThat(properties.get("commit.time")).isEqualTo("2016-03-04 15:22:24");
	}

	@Test
	public void shortenCommitId() {
		GitProperties properties = new GitProperties(
				createProperties("master", "abc", "1457527123"));
		assertThat(properties.getCommitId()).isEqualTo("abc");
		assertThat(properties.getShortCommitId()).isEqualTo("abc");
	}

	private static Properties createProperties(String branch, String commitId,
			String commitTime) {
		Properties properties = new Properties();
		properties.put("branch", branch);
		properties.put("commit.id", commitId);
		properties.put("commit.time", commitTime);
		return properties;
	}

}

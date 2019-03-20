/*
 * Copyright 2012-2016 the original author or authors.
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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BuildProperties}.
 *
 * @author Stephane Nicoll
 */
public class BuildPropertiesTests {

	@Test
	public void basicInfo() {
		BuildProperties properties = new BuildProperties(createProperties("com.example",
				"demo", "0.0.1", "2016-03-04T14:36:33+0100"));
		assertThat(properties.getGroup()).isEqualTo("com.example");
		assertThat(properties.getArtifact()).isEqualTo("demo");
		assertThat(properties.getVersion()).isEqualTo("0.0.1");
		assertThat(properties.getTime()).isNotNull();
		assertThat(properties.get("time")).isEqualTo("1457098593000");
		assertThat(properties.getTime().getTime()).isEqualTo(1457098593000L);
	}

	@Test
	public void noInfo() {
		BuildProperties properties = new BuildProperties(new Properties());
		assertThat(properties.getGroup()).isNull();
		assertThat(properties.getArtifact()).isNull();
		assertThat(properties.getVersion()).isNull();
		assertThat(properties.getTime()).isNull();
	}

	private static Properties createProperties(String group, String artifact,
			String version, String buildTime) {
		Properties properties = new Properties();
		properties.put("group", group);
		properties.put("artifact", artifact);
		properties.put("version", version);
		properties.put("time", buildTime);
		return properties;
	}

}

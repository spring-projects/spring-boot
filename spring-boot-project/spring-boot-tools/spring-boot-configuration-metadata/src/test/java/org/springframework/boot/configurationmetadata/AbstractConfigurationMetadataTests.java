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

package org.springframework.boot.configurationmetadata;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base for configuration meta-data tests.
 *
 * @author Stephane Nicoll
 */
public abstract class AbstractConfigurationMetadataTests {

	protected void assertSource(ConfigurationMetadataSource actual, String groupId, String type, String sourceType) {
		assertThat(actual).isNotNull();
		assertThat(actual.getGroupId()).isEqualTo(groupId);
		assertThat(actual.getType()).isEqualTo(type);
		assertThat(actual.getSourceType()).isEqualTo(sourceType);
	}

	protected void assertProperty(ConfigurationMetadataProperty actual, String id, String name, Class<?> type,
			Object defaultValue) {
		assertThat(actual).isNotNull();
		assertThat(actual.getId()).isEqualTo(id);
		assertThat(actual.getName()).isEqualTo(name);
		String typeName = (type != null) ? type.getName() : null;
		assertThat(actual.getType()).isEqualTo(typeName);
		assertThat(actual.getDefaultValue()).isEqualTo(defaultValue);
	}

	protected void assertItem(ConfigurationMetadataItem actual, String sourceType) {
		assertThat(actual).isNotNull();
		assertThat(actual.getSourceType()).isEqualTo(sourceType);
	}

	protected InputStream getInputStreamFor(String name) throws IOException {
		Resource r = new ClassPathResource("metadata/configuration-metadata-" + name + ".json");
		return r.getInputStream();
	}

}

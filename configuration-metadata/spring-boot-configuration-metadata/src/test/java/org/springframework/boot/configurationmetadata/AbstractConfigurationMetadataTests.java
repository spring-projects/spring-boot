/*
 * Copyright 2012-present the original author or authors.
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

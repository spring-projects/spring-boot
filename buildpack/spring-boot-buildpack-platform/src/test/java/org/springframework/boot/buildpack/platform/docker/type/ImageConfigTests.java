/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.type;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ImageConfig}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ImageConfigTests extends AbstractJsonTests {

	@Test
	void getEnvContainsParsedValues() {
		ImageConfig imageConfig = getImageConfig();
		Map<String, @Nullable String> env = imageConfig.getEnv();
		assertThat(env).contains(entry("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"),
				entry("CNB_USER_ID", "2000"), entry("CNB_GROUP_ID", "2000"),
				entry("CNB_STACK_ID", "org.cloudfoundry.stacks.cflinuxfs3"));
	}

	@Test
	void whenConfigHasNoEnvThenImageConfigEnvIsEmpty() {
		ImageConfig imageConfig = getMinimalImageConfig();
		Map<String, @Nullable String> env = imageConfig.getEnv();
		assertThat(env).isEmpty();
	}

	@Test
	void whenConfigHasNoLabelsThenImageConfigLabelsIsEmpty() {
		ImageConfig imageConfig = getMinimalImageConfig();
		Map<String, String> env = imageConfig.getLabels();
		assertThat(env).isEmpty();
	}

	@Test
	void getLabelsReturnsLabels() {
		ImageConfig imageConfig = getImageConfig();
		Map<String, String> labels = imageConfig.getLabels();
		assertThat(labels).hasSize(4).contains(entry("io.buildpacks.stack.id", "org.cloudfoundry.stacks.cflinuxfs3"));
	}

	@Test
	void updateWithLabelUpdatesLabels() {
		ImageConfig imageConfig = getImageConfig();
		ImageConfig updatedImageConfig = imageConfig
			.copy((update) -> update.withLabel("io.buildpacks.stack.id", "test"));
		assertThat(imageConfig.getLabels()).hasSize(4)
			.contains(entry("io.buildpacks.stack.id", "org.cloudfoundry.stacks.cflinuxfs3"));
		assertThat(updatedImageConfig.getLabels()).hasSize(4).contains(entry("io.buildpacks.stack.id", "test"));
	}

	private ImageConfig getImageConfig() {
		return new ImageConfig(getJsonMapper().readTree(getContent("image-config.json")));
	}

	private ImageConfig getMinimalImageConfig() {
		return new ImageConfig(getJsonMapper().readTree(getContent("minimal-image-config.json")));
	}

}

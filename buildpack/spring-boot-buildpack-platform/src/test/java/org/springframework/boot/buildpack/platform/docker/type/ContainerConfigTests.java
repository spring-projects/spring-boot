/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.type;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ContainerConfig}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
 */
class ContainerConfigTests extends AbstractJsonTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void ofWhenImageReferenceIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ContainerConfig.of(null, (update) -> {
		})).withMessage("'imageReference' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void ofWhenUpdateIsNullThrowsException() {
		ImageReference imageReference = ImageReference.of("ubuntu:bionic");
		assertThatIllegalArgumentException().isThrownBy(() -> ContainerConfig.of(imageReference, null))
			.withMessage("'update' must not be null");
	}

	@Test
	void writeToWritesJson() throws Exception {
		ImageReference imageReference = ImageReference.of("ubuntu:bionic");
		ContainerConfig containerConfig = ContainerConfig.of(imageReference, (update) -> {
			update.withUser("root");
			update.withCommand("ls", "-l");
			update.withArgs("-h");
			update.withLabel("spring", "boot");
			update.withBinding(Binding.from("bind-source", "bind-dest"));
			update.withEnv("name1", "value1");
			update.withEnv("name2", "value2");
			update.withNetworkMode("test");
			update.withSecurityOption("option=value");
		});
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		containerConfig.writeTo(outputStream);
		String actualJson = outputStream.toString(StandardCharsets.UTF_8);
		String expectedJson = StreamUtils.copyToString(getContent("container-config.json"), StandardCharsets.UTF_8);
		JSONAssert.assertEquals(expectedJson, actualJson, true);
	}

}

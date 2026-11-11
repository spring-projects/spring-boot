/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ImagePlatformTests extends AbstractJsonTests {

	@Test
	void ofWithOsParses() {
		ImagePlatform platform = ImagePlatform.of("linux");
		assertThat(platform.toString()).isEqualTo("linux");
	}

	@Test
	void ofWithOsAndArchitectureParses() {
		ImagePlatform platform = ImagePlatform.of("linux/amd64");
		assertThat(platform.toString()).isEqualTo("linux/amd64");
	}

	@Test
	void ofWithOsAndArchitectureAndVariantParses() {
		ImagePlatform platform = ImagePlatform.of("linux/amd64/v1");
		assertThat(platform.toString()).isEqualTo("linux/amd64/v1");
	}

	@Test
	void ofWithEmptyValueFails() {
		assertThatIllegalArgumentException().isThrownBy(() -> ImagePlatform.of(""))
			.withMessageContaining("'value' must not be empty");
	}

	@Test
	void ofWithTooManySegmentsFails() {
		assertThatIllegalArgumentException().isThrownBy(() -> ImagePlatform.of("linux/amd64/v1/extra"))
			.withMessageContaining("'value' [linux/amd64/v1/extra] must be in the form");
	}

	@Test
	void fromImageMatchesImage() throws IOException {
		ImagePlatform platform = ImagePlatform.from(getImage());
		assertThat(platform.toString()).isEqualTo("linux/amd64/v1");
	}

	@Test
	void toJsonString() {
		ImagePlatform platform = ImagePlatform.of("linux/amd64/v1");
		assertThat(platform.toJson()).isEqualTo("""
				{"os":"linux","architecture":"amd64","variant":"v1"}""");
	}

	@Test
	void toJsonStringWhenOnlyOs() {
		ImagePlatform platform = ImagePlatform.of("linux");
		assertThat(platform.toJson()).isEqualTo("""
				{"os":"linux"}""");
	}

	private Image getImage() throws IOException {
		return Image.of(getContent("type/image.json"));
	}

}

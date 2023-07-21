/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.buildpack.platform.build;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StackId}.
 *
 * @author Phillip Webb
 */
class StackIdTests {

	@Test
	void fromImageWhenImageIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> StackId.fromImage(null))
			.withMessage("Image must not be null");
	}

	@Test
	void fromImageWhenLabelIsMissingThrowsException() {
		Image image = mock(Image.class);
		ImageConfig imageConfig = mock(ImageConfig.class);
		given(image.getConfig()).willReturn(imageConfig);
		assertThatIllegalStateException().isThrownBy(() -> StackId.fromImage(image))
			.withMessage("Missing 'io.buildpacks.stack.id' stack label");
	}

	@Test
	void fromImageCreatesStackId() {
		Image image = mock(Image.class);
		ImageConfig imageConfig = mock(ImageConfig.class);
		given(image.getConfig()).willReturn(imageConfig);
		given(imageConfig.getLabels()).willReturn(Collections.singletonMap("io.buildpacks.stack.id", "test"));
		StackId stackId = StackId.fromImage(image);
		assertThat(stackId).hasToString("test");
	}

	@Test
	void ofCreatesStackId() {
		StackId stackId = StackId.of("test");
		assertThat(stackId).hasToString("test");
	}

	@Test
	void equalsAndHashCode() {
		StackId s1 = StackId.of("a");
		StackId s2 = StackId.of("a");
		StackId s3 = StackId.of("b");
		assertThat(s1).hasSameHashCodeAs(s2);
		assertThat(s1).isEqualTo(s1).isEqualTo(s2).isNotEqualTo(s3);
	}

	@Test
	void toStringReturnsValue() {
		StackId stackId = StackId.of("test");
		assertThat(stackId).hasToString("test");
	}

}

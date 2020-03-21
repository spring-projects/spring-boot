/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.loader.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Layer}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class LayerTests {

	@Test
	void createWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Layer(null)).withMessage("Name must not be empty");
	}

	@Test
	void createWhenNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Layer("")).withMessage("Name must not be empty");
	}

	@Test
	void createWhenNameContainsBadCharsThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Layer("bad!name"))
				.withMessage("Malformed layer name 'bad!name'");
	}

	@Test
	void equalsAndHashCode() {
		Layer layer1 = new Layer("testa");
		Layer layer2 = new Layer("testa");
		Layer layer3 = new Layer("testb");
		assertThat(layer1.hashCode()).isEqualTo(layer2.hashCode());
		assertThat(layer1).isEqualTo(layer1).isEqualTo(layer2).isNotEqualTo(layer3);
	}

	@Test
	void toStringReturnsName() {
		assertThat(new Layer("test")).hasToString("test");
	}

	@Test
	void createWhenUsingReservedNameThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Layer("ext"))
				.withMessage("Layer name 'ext' is reserved");
		assertThatIllegalArgumentException().isThrownBy(() -> new Layer("ExT"))
				.withMessage("Layer name 'ExT' is reserved");
		assertThatIllegalArgumentException().isThrownBy(() -> new Layer("springbootloader"))
				.withMessage("Layer name 'springbootloader' is reserved");
	}

}

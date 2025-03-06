/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.container;

import org.junit.jupiter.api.Test;

import org.springframework.core.AttributeAccessor;
import org.springframework.core.AttributeAccessorSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContainerImageMetadata}.
 *
 * @author Phillip Webb
 */
class ContainerImageMetadataTests {

	private ContainerImageMetadata metadata = new ContainerImageMetadata("test");

	private AttributeAccessor attributes = new AttributeAccessorSupport() {

	};

	@Test
	void addToWhenAttributesIsNullDoesNothing() {
		this.metadata.addTo(null);
	}

	@Test
	void addToAddsMetadata() {
		this.metadata.addTo(this.attributes);
		assertThat(this.attributes.getAttribute(ContainerImageMetadata.NAME)).isSameAs(this.metadata);
	}

	@Test
	void isPresentWhenPresentReturnsTrue() {
		this.metadata.addTo(this.attributes);
		assertThat(ContainerImageMetadata.isPresent(this.attributes)).isTrue();
	}

	@Test
	void isPresentWhenNotPresentReturnsFalse() {
		assertThat(ContainerImageMetadata.isPresent(this.attributes)).isFalse();
	}

	@Test
	void isPresentWhenNullAttributesReturnsFalse() {
		assertThat(ContainerImageMetadata.isPresent(null)).isFalse();
	}

	@Test
	void getFromWhenPresentReturnsMetadata() {
		this.metadata.addTo(this.attributes);
		assertThat(ContainerImageMetadata.getFrom(this.attributes)).isSameAs(this.metadata);
	}

	@Test
	void getFromWhenNotPresentReturnsNull() {
		assertThat(ContainerImageMetadata.getFrom(this.attributes)).isNull();
	}

	@Test
	void getFromWhenNullAttributesReturnsNull() {
		assertThat(ContainerImageMetadata.getFrom(null)).isNull();
	}

}

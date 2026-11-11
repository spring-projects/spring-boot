/*
 * Copyright 2012-present the original author or authors.
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

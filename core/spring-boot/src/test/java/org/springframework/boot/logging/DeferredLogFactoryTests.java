/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging;

import org.apache.commons.logging.Log;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DeferredLogFactory}.
 *
 * @author Phillip Webb
 */
class DeferredLogFactoryTests {

	private final DeferredLogFactory factory = (supplier) -> this.log = supplier.get();

	private @Nullable Log log;

	@Test
	void getLogFromClassCreatesLogSupplier() {
		this.factory.getLog(DeferredLogFactoryTests.class);
		assertThat(this.log).isNotNull();
	}

	@Test
	void getLogFromDestinationCreatesLogSupplier() {
		Log log = mock(Log.class);
		this.factory.getLog(log);
		assertThat(this.log).isSameAs(log);
	}

}

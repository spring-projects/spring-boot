/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test customizer that can assert that it has been called.
 *
 * @param <T> type being customized
 * @author Phillip Webb
 */
class TestCustomizer<T> implements Consumer<T> {

	private boolean called;

	@Override
	public void accept(T t) {
		this.called = true;
	}

	void assertCalled() {
		assertThat(this.called).isTrue();
	}

}

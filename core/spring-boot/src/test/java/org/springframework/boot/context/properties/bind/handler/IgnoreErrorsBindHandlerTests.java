/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.bind.handler;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link IgnoreErrorsBindHandler}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class IgnoreErrorsBindHandlerTests {

	private final List<ConfigurationPropertySource> sources = new ArrayList<>();

	private Binder binder;

	@BeforeEach
	void setup() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("example.foo", "bar");
		this.sources.add(source);
		this.binder = new Binder(this.sources);
	}

	@Test
	void bindWhenNotIgnoringErrorsShouldFail() {
		assertThatExceptionOfType(BindException.class)
			.isThrownBy(() -> this.binder.bind("example", Bindable.of(Example.class)));
	}

	@Test
	void bindWhenIgnoringErrorsShouldBind() {
		Example bound = this.binder.bind("example", Bindable.of(Example.class), new IgnoreErrorsBindHandler()).get();
		assertThat(bound.getFoo()).isZero();
	}

	static class Example {

		private int foo;

		int getFoo() {
			return this.foo;
		}

		void setFoo(int foo) {
			this.foo = foo;
		}

	}

}

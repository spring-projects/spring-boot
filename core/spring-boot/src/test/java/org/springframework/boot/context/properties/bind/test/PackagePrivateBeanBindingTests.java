/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.bind.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Binder} using package private Java beans.
 *
 * @author Madhura Bhave
 */
class PackagePrivateBeanBindingTests {

	private final List<ConfigurationPropertySource> sources = new ArrayList<>();

	private Binder binder;

	private ConfigurationPropertyName name;

	@BeforeEach
	void setup() {
		this.binder = new Binder(this.sources);
		this.name = ConfigurationPropertyName.of("foo");
	}

	@Test
	void bindToPackagePrivateClassShouldBindToInstance() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "999");
		this.sources.add(source);
		ExamplePackagePrivateBean bean = this.binder.bind(this.name, Bindable.of(ExamplePackagePrivateBean.class))
			.get();
		assertThat(bean.getBar()).isEqualTo(999);
	}

	static class ExamplePackagePrivateBean {

		private int bar;

		int getBar() {
			return this.bar;
		}

		void setBar(int bar) {
			this.bar = bar;
		}

	}

}

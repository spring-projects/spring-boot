/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.bind;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link BoundPropertiesTrackingBindHandler}.
 *
 * @author Madhura Bhave
 */
@ExtendWith(MockitoExtension.class)
class BoundPropertiesTrackingBindHandlerTests {

	private final List<ConfigurationPropertySource> sources = new ArrayList<>();

	private BoundPropertiesTrackingBindHandler handler;

	private Binder binder;

	@Mock
	private Consumer<ConfigurationProperty> consumer;

	@BeforeEach
	void setup() {
		this.binder = new Binder(this.sources);
		this.handler = new BoundPropertiesTrackingBindHandler(this.consumer);
	}

	@Test
	void handlerShouldCallRecordBindingIfConfigurationPropertyIsNotNull() {
		this.sources.add(new MockConfigurationPropertySource("foo.age", 4));
		this.binder.bind("foo", Bindable.of(ExampleBean.class), this.handler);
		then(this.consumer).should().accept(any(ConfigurationProperty.class));
		then(this.consumer).should(never()).accept(null);
	}

	static class ExampleBean {

		private int age;

		int getAge() {
			return this.age;
		}

		void setAge(int age) {
			this.age = age;
		}

	}

}

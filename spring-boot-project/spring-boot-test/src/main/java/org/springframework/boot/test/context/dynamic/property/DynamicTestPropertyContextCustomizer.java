package org.springframework.boot.test.context.dynamic.property;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

import java.util.List;
import java.util.Objects;


public class DynamicTestPropertyContextCustomizer implements ContextCustomizer {


	private List<TestPropertyValues> properties;

	public DynamicTestPropertyContextCustomizer(List<TestPropertyValues> properties) {
		this.properties = properties;
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext configurableApplicationContext, MergedContextConfiguration mergedContextConfiguration) {
		for (TestPropertyValues property : properties) {
			property.applyTo(configurableApplicationContext);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DynamicTestPropertyContextCustomizer that = (DynamicTestPropertyContextCustomizer) o;
		return Objects.equals(properties, that.properties);
	}

	@Override
	public int hashCode() {
		return Objects.hash(properties);
	}
}

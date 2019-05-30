package org.springframework.boot.test.context.dynamic.property;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * {@link ContextCustomizer} to allow using the {@link DynamicTestProperty} in tests.
 *
 * @author Anatoliy Korovin
 */
public class DynamicTestPropertyContextCustomizer implements ContextCustomizer {


	private Set<TestPropertyValues> properties;

	public DynamicTestPropertyContextCustomizer(List<TestPropertyValues> properties) {
		this.properties =  new HashSet<>(properties);
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

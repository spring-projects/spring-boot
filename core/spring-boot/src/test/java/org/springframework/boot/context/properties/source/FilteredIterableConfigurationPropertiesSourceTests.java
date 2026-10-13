/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import java.util.LinkedHashMap;
import java.util.Map;

import org.assertj.core.extractor.Extractors;
import org.junit.jupiter.api.Test;

import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link FilteredIterableConfigurationPropertiesSource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class FilteredIterableConfigurationPropertiesSourceTests extends FilteredConfigurationPropertiesSourceTests {

	@Test
	void iteratorFiltersNames() {
		MockConfigurationPropertySource source = (MockConfigurationPropertySource) createTestSource();
		IterableConfigurationPropertySource filtered = source.filter(this::noBrackets);
		assertThat(filtered.iterator()).toIterable()
			.extracting(ConfigurationPropertyName::toString)
			.containsExactly("a", "b", "c");
	}

	@Test
	void containsDescendantOfUsesContents() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar.baz", "1");
		source.put("foo.bar[0]", "1");
		source.put("faf.bar[0]", "1");
		IterableConfigurationPropertySource filtered = source.filter(this::noBrackets);
		assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("foo")))
			.isEqualTo(ConfigurationPropertyState.PRESENT);
		assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("faf")))
			.isEqualTo(ConfigurationPropertyState.ABSENT);
	}

	@Test
	void iteratorWhenSpringPropertySourceFiltersNames() {
		IterableConfigurationPropertySource testSource = (IterableConfigurationPropertySource) createTestSource();
		Map<String, Object> map = new LinkedHashMap<>();
		for (ConfigurationPropertyName name : testSource) {
			ConfigurationProperty configurationProperty = testSource.getConfigurationProperty(name);
			assertThat(configurationProperty).isNotNull();
			map.put(name.toString(), configurationProperty.getValue());
		}
		PropertySource<?> propertySource = new OriginTrackedMapPropertySource("test", map, true);
		SpringConfigurationPropertySource source = SpringConfigurationPropertySource.from(propertySource);
		IterableConfigurationPropertySource filtered = (IterableConfigurationPropertySource) source
			.filter(this::noBrackets);
		assertThat(Extractors.byName("filteredNames").apply(filtered)).isNotNull();
		assertThat(filtered.iterator()).toIterable()
			.extracting(ConfigurationPropertyName::toString)
			.containsExactly("a", "b", "c");
	}

	@Test
	void iteratorWhenSpringPropertySourceAndAnotherFilterFiltersNames() {
		IterableConfigurationPropertySource testSource = (IterableConfigurationPropertySource) createTestSource();
		Map<String, Object> map = new LinkedHashMap<>();
		for (ConfigurationPropertyName name : testSource) {
			ConfigurationProperty configurationProperty = testSource.getConfigurationProperty(name);
			assertThat(configurationProperty).isNotNull();
			map.put(name.toString(), configurationProperty.getValue());
		}
		PropertySource<?> propertySource = new OriginTrackedMapPropertySource("test", map, true);
		SpringConfigurationPropertySource source = SpringConfigurationPropertySource.from(propertySource);
		IterableConfigurationPropertySource filtered = (IterableConfigurationPropertySource) source
			.filter(this::noBrackets);
		IterableConfigurationPropertySource secondFiltered = filtered.filter((name) -> !name.toString().contains("c"));
		assertThat(Extractors.byName("filteredNames").apply(filtered)).isNotNull();
		assertThat(secondFiltered.iterator()).toIterable()
			.extracting(ConfigurationPropertyName::toString)
			.containsExactly("a", "b");
	}

	@Test
	void containsDescendantOfWhenSpringPropertySourceUsesContents() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("foo.bar.baz", "1");
		map.put("foo.bar[0]", "1");
		map.put("faf.bar[0]", "1");
		PropertySource<?> propertySource = new OriginTrackedMapPropertySource("test", map, true);
		SpringConfigurationPropertySource source = SpringConfigurationPropertySource.from(propertySource);
		IterableConfigurationPropertySource filtered = (IterableConfigurationPropertySource) source
			.filter(this::noBrackets);
		assertThat(Extractors.byName("filteredNames").apply(filtered)).isNotNull();
		assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("foo")))
			.isEqualTo(ConfigurationPropertyState.PRESENT);
		assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("faf")))
			.isEqualTo(ConfigurationPropertyState.ABSENT);
	}

	@Override
	protected ConfigurationPropertySource convertSource(MockConfigurationPropertySource source) {
		return source;
	}

	private boolean noBrackets(ConfigurationPropertyName name) {
		return !name.toString().contains("[");
	}

}

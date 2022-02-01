/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot;

import java.util.Collections;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link DefaultPropertiesPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@ExtendWith(MockitoExtension.class)
class DefaultPropertiesPropertySourceTests {

	@Mock
	private Consumer<DefaultPropertiesPropertySource> action;

	@Captor
	private ArgumentCaptor<DefaultPropertiesPropertySource> captor;

	@Test
	void nameIsDefaultProperties() {
		assertThat(DefaultPropertiesPropertySource.NAME).isEqualTo("defaultProperties");
	}

	@Test
	void createCreatesSource() {
		DefaultPropertiesPropertySource propertySource = new DefaultPropertiesPropertySource(
				Collections.singletonMap("spring", "boot"));
		assertThat(propertySource.getName()).isEqualTo("defaultProperties");
		assertThat(propertySource.getProperty("spring")).isEqualTo("boot");
	}

	@Test
	void hasMatchingNameWhenNameMatchesReturnsTrue() {
		MockPropertySource propertySource = new MockPropertySource("defaultProperties");
		assertThat(DefaultPropertiesPropertySource.hasMatchingName(propertySource)).isTrue();
	}

	@Test
	void hasMatchingNameWhenNameDoesNotMatchReturnsFalse() {
		MockPropertySource propertySource = new MockPropertySource("normalProperties");
		assertThat(DefaultPropertiesPropertySource.hasMatchingName(propertySource)).isFalse();
	}

	@Test
	void hasMatchingNameWhenPropertySourceIsNullReturnsFalse() {
		assertThat(DefaultPropertiesPropertySource.hasMatchingName(null)).isFalse();
	}

	@Test
	void ifNotEmptyWhenNullDoesNotCallAction() {
		DefaultPropertiesPropertySource.ifNotEmpty(null, this.action);
		then(this.action).shouldHaveNoInteractions();
	}

	@Test
	void ifNotEmptyWhenEmptyDoesNotCallAction() {
		DefaultPropertiesPropertySource.ifNotEmpty(Collections.emptyMap(), this.action);
		then(this.action).shouldHaveNoInteractions();
	}

	@Test
	void ifNotEmptyHasValueCallsAction() {
		DefaultPropertiesPropertySource.ifNotEmpty(Collections.singletonMap("spring", "boot"), this.action);
		then(this.action).should().accept(this.captor.capture());
		assertThat(this.captor.getValue().getProperty("spring")).isEqualTo("boot");
	}

	@Test
	void moveToEndWhenNotPresentDoesNothing() {
		MockEnvironment environment = new MockEnvironment();
		DefaultPropertiesPropertySource.moveToEnd(environment);
	}

	@Test
	void addOrMergeWhenExistingNotFoundShouldAdd() {
		MockEnvironment environment = new MockEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		DefaultPropertiesPropertySource.addOrMerge(Collections.singletonMap("spring", "boot"), propertySources);
		assertThat(propertySources.contains(DefaultPropertiesPropertySource.NAME)).isTrue();
		assertThat(propertySources.get(DefaultPropertiesPropertySource.NAME).getProperty("spring")).isEqualTo("boot");
	}

	@Test
	void addOrMergeWhenExistingFoundShouldMerge() {
		MockEnvironment environment = new MockEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		propertySources.addLast(new DefaultPropertiesPropertySource(Collections.singletonMap("spring", "boot")));
		DefaultPropertiesPropertySource.addOrMerge(Collections.singletonMap("hello", "world"), propertySources);
		assertThat(propertySources.contains(DefaultPropertiesPropertySource.NAME)).isTrue();
		assertThat(propertySources.get(DefaultPropertiesPropertySource.NAME).getProperty("spring")).isEqualTo("boot");
		assertThat(propertySources.get(DefaultPropertiesPropertySource.NAME).getProperty("hello")).isEqualTo("world");
	}

	@Test
	void addOrMergeWhenExistingNotMapPropertySourceShouldNotMerge() {
		MockEnvironment environment = new MockEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		CompositePropertySource composite = new CompositePropertySource(DefaultPropertiesPropertySource.NAME);
		composite.addPropertySource(new DefaultPropertiesPropertySource(Collections.singletonMap("spring", "boot")));
		propertySources.addFirst(composite);
		DefaultPropertiesPropertySource.addOrMerge(Collections.singletonMap("hello", "world"), propertySources);
		assertThat(propertySources.contains(DefaultPropertiesPropertySource.NAME)).isTrue();
		assertThat(propertySources.get(DefaultPropertiesPropertySource.NAME).getProperty("spring")).isNull();
		assertThat(propertySources.get(DefaultPropertiesPropertySource.NAME).getProperty("hello")).isEqualTo("world");
	}

	@Test
	void moveToEndWhenPresentMovesToEnd() {
		MockEnvironment environment = new MockEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		propertySources.addLast(new DefaultPropertiesPropertySource(Collections.singletonMap("spring", "boot")));
		propertySources.addLast(new MockPropertySource("test"));
		DefaultPropertiesPropertySource.moveToEnd(environment);
		String[] names = propertySources.stream().map(PropertySource::getName).toArray(String[]::new);
		assertThat(names).containsExactly(MockPropertySource.MOCK_PROPERTIES_PROPERTY_SOURCE_NAME, "test",
				DefaultPropertiesPropertySource.NAME);
	}

}

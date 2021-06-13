/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.context.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Profiles}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ProfilesTests {

	@Test
	void getActiveWhenNoEnvironmentProfilesAndNoPropertyReturnsEmptyArray() {
		Environment environment = new MockEnvironment();
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getActive()).isEmpty();
	}

	@Test
	void getActiveWhenNoEnvironmentProfilesAndBinderProperty() {
		Environment environment = new MockEnvironment();
		Binder binder = new Binder(
				new MapConfigurationPropertySource(Collections.singletonMap("spring.profiles.active", "a,b,c")));
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getActive()).containsExactly("a", "b", "c");
	}

	@Test
	void getActiveWhenNoEnvironmentProfilesAndEnvironmentProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.active", "a,b,c");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getActive()).containsExactly("a", "b", "c");
	}

	@Test
	void getActiveWhenEnvironmentProfilesAndBinderProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("a", "b", "c");
		Binder binder = new Binder(
				new MapConfigurationPropertySource(Collections.singletonMap("spring.profiles.active", "d,e,f")));
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getActive()).containsExactly("a", "b", "c", "d", "e", "f");
	}

	@Test
	void getActiveWhenEnvironmentProfilesAndBinderPropertyShouldReturnEnvironmentProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.active", "a,b,c");
		List<ConfigurationPropertySource> sources = new ArrayList<>();
		ConfigurationPropertySources.get(environment).forEach(sources::add);
		sources.add(new MapConfigurationPropertySource(Collections.singletonMap("spring.profiles.active", "d,e,f")));
		Binder binder = new Binder(sources);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getActive()).containsExactly("a", "b", "c");
	}

	@Test
	void getActiveWhenEnvironmentProfilesAndEnvironmentProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("a", "b", "c");
		environment.setProperty("spring.profiles.active", "d,e,f");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getActive()).containsExactly("a", "b", "c", "d", "e", "f");
	}

	@Test
	void getActiveWhenNoEnvironmentProfilesAndEnvironmentPropertyInBindNotation() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.active[0]", "a");
		environment.setProperty("spring.profiles.active[1]", "b");
		environment.setProperty("spring.profiles.active[2]", "c");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getActive()).containsExactly("a", "b", "c");
	}

	@Test
	void getActiveWhenEnvironmentProfilesInBindNotationAndEnvironmentPropertyReturnsEnvironmentProfiles() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("a", "b", "c");
		environment.setProperty("spring.profiles.active[0]", "d");
		environment.setProperty("spring.profiles.active[1]", "e");
		environment.setProperty("spring.profiles.active[2]", "f");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getActive()).containsExactly("a", "b", "c", "d", "e", "f");
	}

	@Test
	void getActiveWhenHasDuplicatesReturnsUniqueElements() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.active", "a,b,a,b,c");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getActive()).containsExactly("a", "b", "c");
	}

	@Test
	void getActiveWithProfileGroups() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.active", "a,b,c");
		environment.setProperty("spring.profiles.group.a", "d,e");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getActive()).containsExactly("a", "d", "e", "b", "c");
	}

	@Test
	void getActiveWhenHasAdditionalIncludesAdditional() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.active", "d,e,f");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, Arrays.asList("a", "b", "c"));
		assertThat(profiles.getActive()).containsExactly("a", "b", "c", "d", "e", "f");
	}

	@Test
	void getDefaultWhenNoEnvironmentProfilesAndNoPropertyReturnsEmptyArray() {
		Environment environment = new MockEnvironment();
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getDefault()).containsExactly("default");
	}

	@Test
	void getDefaultWhenNoEnvironmentProfilesAndBinderProperty() {
		Environment environment = new MockEnvironment();
		Binder binder = new Binder(
				new MapConfigurationPropertySource(Collections.singletonMap("spring.profiles.default", "a,b,c")));
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getDefault()).containsExactly("a", "b", "c");
	}

	@Test
	void getDefaultWhenDefaultEnvironmentProfileAndBinderProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.default", "default");
		List<ConfigurationPropertySource> sources = new ArrayList<>();
		ConfigurationPropertySources.get(environment).forEach(sources::add);
		sources.add(new MapConfigurationPropertySource(Collections.singletonMap("spring.profiles.default", "a,b,c")));
		Binder binder = new Binder(sources);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getDefault()).containsExactly("default");
	}

	@Test
	void getDefaultWhenNoEnvironmentProfilesAndEnvironmentProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.default", "a,b,c");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getDefault()).containsExactly("a", "b", "c");
	}

	@Test
	void getDefaultWhenEnvironmentProfilesAndBinderProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setDefaultProfiles("a", "b", "c");
		Binder binder = new Binder(
				new MapConfigurationPropertySource(Collections.singletonMap("spring.profiles.default", "d,e,f")));
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getDefault()).containsExactly("a", "b", "c");
	}

	@Test
	void getDefaultWhenEnvironmentProfilesAndEnvironmentProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setDefaultProfiles("a", "b", "c");
		environment.setProperty("spring.profiles.default", "d,e,f");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getDefault()).containsExactly("a", "b", "c");
	}

	@Test
	void getDefaultWhenNoEnvironmentProfilesAndEnvironmentPropertyInBindNotation() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.default[0]", "a");
		environment.setProperty("spring.profiles.default[1]", "b");
		environment.setProperty("spring.profiles.default[2]", "c");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getDefault()).containsExactly("a", "b", "c");
	}

	@Test
	void getDefaultWhenHasDuplicatesReturnsUniqueElements() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.default", "a,b,a,b,c");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getDefault()).containsExactly("a", "b", "c");
	}

	@Test
	void getDefaultWithProfileGroups() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.default", "a,b,c");
		environment.setProperty("spring.profiles.group.a", "d,e");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getDefault()).containsExactly("a", "d", "e", "b", "c");
	}

	@Test
	void getDefaultWhenEnvironmentProfilesInBindNotationAndEnvironmentPropertyReturnsBoth() {
		MockEnvironment environment = new MockEnvironment();
		environment.setDefaultProfiles("a", "b", "c");
		environment.setProperty("spring.profiles.default[0]", "d");
		environment.setProperty("spring.profiles.default[1]", "e");
		environment.setProperty("spring.profiles.default[2]", "f");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getDefault()).containsExactly("a", "b", "c");
	}

	@Test
	void iteratorIteratesAllActiveProfiles() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("a", "b", "c");
		environment.setDefaultProfiles("d", "e", "f");
		Binder binder = Binder.get(environment);
		Profiles profiles1 = new Profiles(environment, binder, null);
		Profiles profiles = profiles1;
		assertThat(profiles).containsExactly("a", "b", "c");
	}

	@Test
	void iteratorIteratesAllDefaultProfilesWhenNoActive() {
		MockEnvironment environment = new MockEnvironment();
		environment.setDefaultProfiles("d", "e", "f");
		Binder binder = Binder.get(environment);
		Profiles profiles1 = new Profiles(environment, binder, null);
		Profiles profiles = profiles1;
		assertThat(profiles).containsExactly("d", "e", "f");
	}

	@Test
	void isActiveWhenActiveContainsProfileReturnsTrue() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("a", "b", "c");
		Binder binder = Binder.get(environment);
		Profiles profiles1 = new Profiles(environment, binder, null);
		Profiles profiles = profiles1;
		assertThat(profiles.isAccepted("a")).isTrue();
	}

	@Test
	void isActiveWhenActiveDoesNotContainProfileReturnsFalse() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("a", "b", "c");
		Binder binder = Binder.get(environment);
		Profiles profiles1 = new Profiles(environment, binder, null);
		Profiles profiles = profiles1;
		assertThat(profiles.isAccepted("x")).isFalse();
	}

	@Test
	void isActiveWhenNoActiveAndDefaultContainsProfileReturnsTrue() {
		MockEnvironment environment = new MockEnvironment();
		environment.setDefaultProfiles("d", "e", "f");
		Binder binder = Binder.get(environment);
		Profiles profiles1 = new Profiles(environment, binder, null);
		Profiles profiles = profiles1;
		assertThat(profiles.isAccepted("d")).isTrue();
	}

	@Test
	void isActiveWhenNoActiveAndDefaultDoesNotContainProfileReturnsFalse() {
		MockEnvironment environment = new MockEnvironment();
		environment.setDefaultProfiles("d", "e", "f");
		Binder binder = Binder.get(environment);
		Profiles profiles1 = new Profiles(environment, binder, null);
		Profiles profiles = profiles1;
		assertThat(profiles.isAccepted("x")).isFalse();
	}

	@Test
	void iteratorWithProfileGroups() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.active", "a,b,c");
		environment.setProperty("spring.profiles.group.a", "e,f");
		environment.setProperty("spring.profiles.group.e", "x,y");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles).containsExactly("a", "e", "x", "y", "f", "b", "c");
	}

	@Test
	void iteratorWithProfileGroupsAndNoActive() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.group.a", "e,f");
		environment.setProperty("spring.profiles.group.e", "x,y");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles).containsExactly("default");
	}

	@Test
	void iteratorWithProfileGroupsForDefault() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.group.default", "e,f");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles).containsExactly("default", "e", "f");
	}

	@Test
	void getAcceptedWithProfileGroups() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.active", "a,b,c");
		environment.setProperty("spring.profiles.group.a", "e,f");
		environment.setProperty("spring.profiles.group.e", "x,y");
		environment.setDefaultProfiles("g", "h", "i");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getAccepted()).containsExactly("a", "e", "x", "y", "f", "b", "c");
	}

	@Test
	void getAcceptedWhenNoActiveAndDefaultWithGroups() {
		MockEnvironment environment = new MockEnvironment();
		environment.setDefaultProfiles("d", "e", "f");
		environment.setProperty("spring.profiles.group.e", "x,y");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getAccepted()).containsExactly("d", "e", "x", "y", "f");
	}

	@Test
	void isAcceptedWithGroupsReturnsTrue() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.active", "a,b,c");
		environment.setProperty("spring.profiles.group.a", "e,f");
		environment.setProperty("spring.profiles.group.e", "x,y");
		environment.setDefaultProfiles("g", "h", "i");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.isAccepted("a")).isTrue();
		assertThat(profiles.isAccepted("e")).isTrue();
		assertThat(profiles.isAccepted("g")).isFalse();
	}

	@Test
	void isAcceptedWhenNoActiveAndDefaultWithGroupsContainsProfileReturnsTrue() {
		MockEnvironment environment = new MockEnvironment();
		environment.setDefaultProfiles("d", "e", "f");
		environment.setProperty("spring.profiles.group.e", "x,y");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.isAccepted("d")).isTrue();
		assertThat(profiles.isAccepted("x")).isTrue();
	}

	@Test
	void simpleRecursiveReferenceInProfileGroupIgnoresDuplicates() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.active", "a,b,c");
		environment.setProperty("spring.profiles.group.a", "a,e,f");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getAccepted()).containsExactly("a", "e", "f", "b", "c");
	}

	@Test
	void multipleRecursiveReferenceInProfileGroupIgnoresDuplicates() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.active", "a,b,c");
		environment.setProperty("spring.profiles.group.a", "a,b,f");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getAccepted()).containsExactly("a", "b", "f", "c");
	}

	@Test
	void complexRecursiveReferenceInProfileGroupIgnoresDuplicates() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.profiles.active", "a,b,c");
		environment.setProperty("spring.profiles.group.a", "e,f,g");
		environment.setProperty("spring.profiles.group.e", "a,x,y,g");
		Binder binder = Binder.get(environment);
		Profiles profiles = new Profiles(environment, binder, null);
		assertThat(profiles.getAccepted()).containsExactly("a", "e", "x", "y", "g", "f", "b", "c");
	}

}

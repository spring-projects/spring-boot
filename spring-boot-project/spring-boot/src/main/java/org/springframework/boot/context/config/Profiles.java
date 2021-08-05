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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Provides access to environment profiles that have either been set directly on the
 * {@link Environment} or will be set based on configuration data property values.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.4.0
 */
public class Profiles implements Iterable<String> {

	/**
	 * Name of property to set to specify additionally included active profiles.
	 */
	public static final String INCLUDE_PROFILES_PROPERTY_NAME = "spring.profiles.include";

	static final ConfigurationPropertyName INCLUDE_PROFILES = ConfigurationPropertyName
			.of(Profiles.INCLUDE_PROFILES_PROPERTY_NAME);

	private static final Bindable<MultiValueMap<String, String>> STRING_STRINGS_MAP = Bindable
			.of(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class));

	private static final Bindable<Set<String>> STRING_SET = Bindable.setOf(String.class);

	private final MultiValueMap<String, String> groups;

	private final List<String> activeProfiles;

	private final List<String> defaultProfiles;

	/**
	 * Create a new {@link Profiles} instance based on the {@link Environment} and
	 * {@link Binder}.
	 * @param environment the source environment
	 * @param binder the binder for profile properties
	 * @param additionalProfiles any additional active profiles
	 */
	Profiles(Environment environment, Binder binder, Collection<String> additionalProfiles) {
		this.groups = binder.bind("spring.profiles.group", STRING_STRINGS_MAP).orElseGet(LinkedMultiValueMap::new);
		this.activeProfiles = expandProfiles(getActivatedProfiles(environment, binder, additionalProfiles));
		this.defaultProfiles = expandProfiles(getDefaultProfiles(environment, binder));
	}

	private List<String> getActivatedProfiles(Environment environment, Binder binder,
			Collection<String> additionalProfiles) {
		return asUniqueItemList(getProfiles(environment, binder, Type.ACTIVE), additionalProfiles);
	}

	private List<String> getDefaultProfiles(Environment environment, Binder binder) {
		return asUniqueItemList(getProfiles(environment, binder, Type.DEFAULT));
	}

	private Collection<String> getProfiles(Environment environment, Binder binder, Type type) {
		String environmentPropertyValue = environment.getProperty(type.getName());
		Set<String> environmentPropertyProfiles = (!StringUtils.hasLength(environmentPropertyValue))
				? Collections.emptySet()
				: StringUtils.commaDelimitedListToSet(StringUtils.trimAllWhitespace(environmentPropertyValue));
		Set<String> environmentProfiles = new LinkedHashSet<>(Arrays.asList(type.get(environment)));
		BindResult<Set<String>> boundProfiles = binder.bind(type.getName(), STRING_SET);
		if (hasProgrammaticallySetProfiles(type, environmentPropertyValue, environmentPropertyProfiles,
				environmentProfiles)) {
			if (!type.isMergeWithEnvironmentProfiles() || !boundProfiles.isBound()) {
				return environmentProfiles;
			}
			return boundProfiles.map((bound) -> merge(environmentProfiles, bound)).get();
		}
		return boundProfiles.orElse(type.getDefaultValue());
	}

	private boolean hasProgrammaticallySetProfiles(Type type, String environmentPropertyValue,
			Set<String> environmentPropertyProfiles, Set<String> environmentProfiles) {
		if (!StringUtils.hasLength(environmentPropertyValue)) {
			return !type.getDefaultValue().equals(environmentProfiles);
		}
		if (type.getDefaultValue().equals(environmentProfiles)) {
			return false;
		}
		return !environmentPropertyProfiles.equals(environmentProfiles);
	}

	private Set<String> merge(Set<String> environmentProfiles, Set<String> bound) {
		Set<String> result = new LinkedHashSet<>(environmentProfiles);
		result.addAll(bound);
		return result;
	}

	private List<String> expandProfiles(List<String> profiles) {
		Deque<String> stack = new ArrayDeque<>();
		asReversedList(profiles).forEach(stack::push);
		Set<String> expandedProfiles = new LinkedHashSet<>();
		while (!stack.isEmpty()) {
			String current = stack.pop();
			if (expandedProfiles.add(current)) {
				asReversedList(this.groups.get(current)).forEach(stack::push);
			}
		}
		return asUniqueItemList(expandedProfiles);
	}

	private List<String> asReversedList(List<String> list) {
		if (CollectionUtils.isEmpty(list)) {
			return Collections.emptyList();
		}
		List<String> reversed = new ArrayList<>(list);
		Collections.reverse(reversed);
		return reversed;
	}

	private List<String> asUniqueItemList(Collection<String> profiles) {
		return asUniqueItemList(profiles, null);
	}

	private List<String> asUniqueItemList(Collection<String> profiles, Collection<String> additional) {
		LinkedHashSet<String> uniqueItems = new LinkedHashSet<>();
		if (!CollectionUtils.isEmpty(additional)) {
			uniqueItems.addAll(additional);
		}
		uniqueItems.addAll(profiles);
		return Collections.unmodifiableList(new ArrayList<>(uniqueItems));
	}

	/**
	 * Return an iterator for all {@link #getAccepted() accepted profiles}.
	 */
	@Override
	public Iterator<String> iterator() {
		return getAccepted().iterator();
	}

	/**
	 * Return the active profiles.
	 * @return the active profiles
	 */
	public List<String> getActive() {
		return this.activeProfiles;
	}

	/**
	 * Return the default profiles.
	 * @return the active profiles
	 */
	public List<String> getDefault() {
		return this.defaultProfiles;
	}

	/**
	 * Return the accepted profiles.
	 * @return the accepted profiles
	 */
	public List<String> getAccepted() {
		return (!this.activeProfiles.isEmpty()) ? this.activeProfiles : this.defaultProfiles;
	}

	/**
	 * Return if the given profile is active.
	 * @param profile the profile to test
	 * @return if the profile is active
	 */
	public boolean isAccepted(String profile) {
		return getAccepted().contains(profile);
	}

	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this);
		creator.append("active", getActive().toString());
		creator.append("default", getDefault().toString());
		creator.append("accepted", getAccepted().toString());
		return creator.toString();
	}

	/**
	 * A profiles type that can be obtained.
	 */
	private enum Type {

		ACTIVE(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, Environment::getActiveProfiles, true,
				Collections.emptySet()),

		DEFAULT(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME, Environment::getDefaultProfiles, false,
				Collections.singleton("default"));

		private final Function<Environment, String[]> getter;

		private final boolean mergeWithEnvironmentProfiles;

		private final String name;

		private final Set<String> defaultValue;

		Type(String name, Function<Environment, String[]> getter, boolean mergeWithEnvironmentProfiles,
				Set<String> defaultValue) {
			this.name = name;
			this.getter = getter;
			this.mergeWithEnvironmentProfiles = mergeWithEnvironmentProfiles;
			this.defaultValue = defaultValue;
		}

		String getName() {
			return this.name;
		}

		String[] get(Environment environment) {
			return this.getter.apply(environment);
		}

		Set<String> getDefaultValue() {
			return this.defaultValue;
		}

		boolean isMergeWithEnvironmentProfiles() {
			return this.mergeWithEnvironmentProfiles;
		}

	}

}

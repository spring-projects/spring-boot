/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Test implementation for {@link HealthEndpointGroupsRegistry}
 *
 * @author Brian Clozel
 */
public class TestHealthEndpointGroupsRegistry implements HealthEndpointGroupsRegistry {

	private TestHealthEndpointGroup primary = new TestHealthEndpointGroup();

	private Map<String, TestHealthEndpointGroup> groups = new HashMap<>();

	@Override
	public HealthEndpointGroupsRegistry add(String groupName, Consumer<HealthEndpointGroupConfigurer> builder) {
		TestHealthEndpointGroupConfigurer configurer = new TestHealthEndpointGroupConfigurer();
		builder.accept(configurer);
		this.groups.put(groupName, configurer.toHealthEndpointGroup());
		return this;
	}

	@Override
	public HealthEndpointGroupsRegistry remove(String groupName) {
		this.groups.remove(groupName);
		return this;
	}

	@Override
	public HealthEndpointGroups toGroups() {
		return this;
	}

	@Override
	public HealthEndpointGroup getPrimary() {
		return this.primary;
	}

	@Override
	public Set<String> getNames() {
		return this.groups.keySet();
	}

	@Override
	public HealthEndpointGroup get(String name) {
		return this.groups.get(name);
	}

	class TestHealthEndpointGroupConfigurer implements HealthEndpointGroupConfigurer {

		private Predicate<String> predicate = (name) -> true;

		@Override
		public HealthEndpointGroupConfigurer include(String... indicators) {
			Predicate<String> included = Arrays.asList(indicators).stream()
					.map((group) -> (Predicate<String>) (s) -> s.equals(group)).reduce(Predicate::or)
					.orElse((s) -> true);
			this.predicate = this.predicate.and(included);
			return this;
		}

		@Override
		public HealthEndpointGroupConfigurer exclude(String... indicators) {
			Predicate<String> excluded = Arrays.asList(indicators).stream()
					.map((group) -> (Predicate<String>) (s) -> !s.equals(group)).reduce(Predicate::or)
					.orElse((s) -> true);
			this.predicate = this.predicate.and(excluded);
			return this;
		}

		@Override
		public HealthEndpointGroupConfigurer statusAggregator(StatusAggregator statusAggregator) {
			throw new UnsupportedOperationException();
		}

		@Override
		public HealthEndpointGroupConfigurer httpCodeStatusMapper(HttpCodeStatusMapper httpCodeStatusMapper) {
			throw new UnsupportedOperationException();
		}

		@Override
		public HealthEndpointGroupConfigurer showComponents(HealthEndpointGroup.Show showComponents) {
			throw new UnsupportedOperationException();
		}

		@Override
		public HealthEndpointGroupConfigurer showDetails(HealthEndpointGroup.Show showDetails) {
			throw new UnsupportedOperationException();
		}

		@Override
		public HealthEndpointGroupConfigurer roles(String... roles) {
			throw new UnsupportedOperationException();
		}

		TestHealthEndpointGroup toHealthEndpointGroup() {
			return new TestHealthEndpointGroup(this.predicate);
		}

	}

}

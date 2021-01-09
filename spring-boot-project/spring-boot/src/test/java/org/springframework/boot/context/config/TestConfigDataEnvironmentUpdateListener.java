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

package org.springframework.boot.context.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.core.env.PropertySource;

class TestConfigDataEnvironmentUpdateListener implements ConfigDataEnvironmentUpdateListener {

	private final List<AddedPropertySource> addedPropertySources = new ArrayList<>();

	private Profiles profiles;

	@Override
	public void onPropertySourceAdded(PropertySource<?> propertySource, ConfigDataLocation location,
			ConfigDataResource resource) {
		this.addedPropertySources.add(new AddedPropertySource(propertySource, location, resource));
	}

	@Override
	public void onSetProfiles(Profiles profiles) {
		this.profiles = profiles;
	}

	List<AddedPropertySource> getAddedPropertySources() {
		return Collections.unmodifiableList(this.addedPropertySources);
	}

	Profiles getProfiles() {
		return this.profiles;
	}

	static class AddedPropertySource {

		private final PropertySource<?> propertySource;

		private final ConfigDataLocation location;

		private final ConfigDataResource resource;

		AddedPropertySource(PropertySource<?> propertySource, ConfigDataLocation location,
				ConfigDataResource resource) {
			this.propertySource = propertySource;
			this.location = location;
			this.resource = resource;
		}

		PropertySource<?> getPropertySource() {
			return this.propertySource;
		}

		ConfigDataLocation getLocation() {
			return this.location;
		}

		ConfigDataResource getResource() {
			return this.resource;
		}

	}

}

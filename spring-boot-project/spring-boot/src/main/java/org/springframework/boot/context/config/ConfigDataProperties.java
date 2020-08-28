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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.BoundPropertiesTrackingBindHandler;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.origin.Origin;
import org.springframework.util.ObjectUtils;

/**
 * Bound properties used when working with {@link ConfigData}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataProperties {

	private static final ConfigurationPropertyName NAME = ConfigurationPropertyName.of("spring.config");

	private static final ConfigurationPropertyName IMPORT_NAME = ConfigurationPropertyName.of("spring.config.import");

	private static final ConfigurationPropertyName LEGACY_PROFILES_NAME = ConfigurationPropertyName
			.of("spring.profiles");

	private static final Bindable<ConfigDataProperties> BINDABLE_PROPERTIES = Bindable.of(ConfigDataProperties.class);

	private static final Bindable<String[]> BINDABLE_STRING_ARRAY = Bindable.of(String[].class);

	private final List<String> imports;

	private final Activate activate;

	private final Map<ConfigurationPropertyName, ConfigurationProperty> boundProperties;

	/**
	 * Create a new {@link ConfigDataProperties} instance.
	 * @param imports the imports requested
	 * @param activate the activate properties
	 */
	ConfigDataProperties(@Name("import") List<String> imports, Activate activate) {
		this(imports, activate, Collections.emptyList());
	}

	private ConfigDataProperties(List<String> imports, Activate activate, List<ConfigurationProperty> boundProperties) {
		this.imports = (imports != null) ? imports : Collections.emptyList();
		this.activate = activate;
		this.boundProperties = mapByName(boundProperties);
	}

	private Map<ConfigurationPropertyName, ConfigurationProperty> mapByName(
			List<ConfigurationProperty> boundProperties) {
		Map<ConfigurationPropertyName, ConfigurationProperty> result = new LinkedHashMap<>();
		boundProperties.forEach((property) -> result.put(property.getName(), property));
		return Collections.unmodifiableMap(result);
	}

	/**
	 * Return any additional imports requested.
	 * @return the requested imports
	 */
	List<String> getImports() {
		return this.imports;
	}

	/**
	 * Return the {@link Origin} of a given import location.
	 * @param importLocation the import location to check
	 * @return the origin of the import or {@code null}
	 */
	Origin getImportOrigin(String importLocation) {
		int index = this.imports.indexOf(importLocation);
		if (index == -1) {
			return null;
		}
		ConfigurationProperty bound = this.boundProperties.get(IMPORT_NAME.append("[" + index + "]"));
		bound = (bound != null) ? bound : this.boundProperties.get(IMPORT_NAME);
		return (bound != null) ? bound.getOrigin() : null;
	}

	/**
	 * Return {@code true} if the properties indicate that the config data property source
	 * is active for the given activation context.
	 * @param activationContext the activation context
	 * @return {@code true} if the config data property source is active
	 */
	boolean isActive(ConfigDataActivationContext activationContext) {
		return this.activate == null || this.activate.isActive(activationContext);
	}

	/**
	 * Return a new variant of these properties without any imports.
	 * @return a new {@link ConfigDataProperties} instance
	 */
	ConfigDataProperties withoutImports() {
		return new ConfigDataProperties(null, this.activate);
	}

	ConfigDataProperties withLegacyProfiles(String[] legacyProfiles, ConfigurationProperty property) {
		if (this.activate != null && !ObjectUtils.isEmpty(this.activate.onProfile)) {
			throw new InvalidConfigDataPropertyException(property, NAME.append("activate.on-profile"), null);
		}
		return new ConfigDataProperties(this.imports, new Activate(this.activate.onCloudPlatform, legacyProfiles));
	}

	ConfigDataProperties withBoundProperties(List<ConfigurationProperty> boundProperties) {
		return new ConfigDataProperties(this.imports, this.activate, boundProperties);
	}

	/**
	 * Factory method used to create {@link ConfigDataProperties} from the given
	 * {@link Binder}.
	 * @param binder the binder used to bind the properties
	 * @return a {@link ConfigDataProperties} instance or {@code null}
	 */
	static ConfigDataProperties get(Binder binder) {
		LegacyProfilesBindHandler legacyProfilesBindHandler = new LegacyProfilesBindHandler();
		String[] legacyProfiles = binder.bind(LEGACY_PROFILES_NAME, BINDABLE_STRING_ARRAY, legacyProfilesBindHandler)
				.orElse(null);
		List<ConfigurationProperty> boundProperties = new ArrayList<>();
		ConfigDataProperties properties = binder
				.bind(NAME, BINDABLE_PROPERTIES, new BoundPropertiesTrackingBindHandler(boundProperties::add))
				.orElse(null);
		if (!ObjectUtils.isEmpty(legacyProfiles)) {
			properties = (properties != null)
					? properties.withLegacyProfiles(legacyProfiles, legacyProfilesBindHandler.getProperty())
					: new ConfigDataProperties(null, new Activate(null, legacyProfiles));
		}
		return (properties != null) ? properties.withBoundProperties(boundProperties) : null;
	}

	/**
	 * {@link BindHandler} used to check for legacy processing properties.
	 */
	private static class LegacyProfilesBindHandler implements BindHandler {

		private ConfigurationProperty property;

		@Override
		public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context,
				Object result) {
			this.property = context.getConfigurationProperty();
			return result;
		}

		ConfigurationProperty getProperty() {
			return this.property;
		}

	}

	/**
	 * Activate properties used to determine when a config data property source is active.
	 */
	static class Activate {

		private final CloudPlatform onCloudPlatform;

		private final String[] onProfile;

		/**
		 * Create a new {@link Activate} instance.
		 * @param onCloudPlatform the cloud platform required for activation
		 * @param onProfile the profile expression required for activation
		 */
		Activate(CloudPlatform onCloudPlatform, String[] onProfile) {
			this.onProfile = onProfile;
			this.onCloudPlatform = onCloudPlatform;
		}

		/**
		 * Return {@code true} if the properties indicate that the config data property
		 * source is active for the given activation context.
		 * @param activationContext the activation context
		 * @return {@code true} if the config data property source is active
		 */
		boolean isActive(ConfigDataActivationContext activationContext) {
			if (activationContext == null) {
				return false;
			}
			boolean activate = true;
			activate = activate && isActive(activationContext.getCloudPlatform());
			activate = activate && isActive(activationContext.getProfiles());
			return activate;
		}

		private boolean isActive(CloudPlatform cloudPlatform) {
			return this.onCloudPlatform == null || this.onCloudPlatform == cloudPlatform;
		}

		private boolean isActive(Profiles profiles) {
			return ObjectUtils.isEmpty(this.onProfile)
					|| (profiles != null && matchesActiveProfiles(profiles::isAccepted));
		}

		private boolean matchesActiveProfiles(Predicate<String> activeProfiles) {
			return org.springframework.core.env.Profiles.of(this.onProfile).matches(activeProfiles);
		}

	}

}

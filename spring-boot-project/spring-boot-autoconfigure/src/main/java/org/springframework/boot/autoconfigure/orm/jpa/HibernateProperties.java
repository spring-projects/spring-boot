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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Hibernate.
 *
 * @author Stephane Nicoll
 * @author Chris Bono
 * @since 2.1.0
 * @see JpaProperties
 */
@ConfigurationProperties("spring.jpa.hibernate")
public class HibernateProperties {

	private static final String DISABLED_SCANNER_CLASS = "org.hibernate.boot.archive.scan.internal.DisabledScanner";

	private final Naming naming = new Naming();

	/**
	 * DDL mode. This is actually a shortcut for the "hibernate.hbm2ddl.auto" property.
	 * Defaults to "create-drop" when using an embedded database and no schema manager was
	 * detected. Otherwise, defaults to "none".
	 */
	private String ddlAuto;

	/**
	 * Returns the value of the ddlAuto property.
	 * @return the value of the ddlAuto property
	 */
	public String getDdlAuto() {
		return this.ddlAuto;
	}

	/**
	 * Sets the value of the ddlAuto property.
	 * @param ddlAuto the value to set for ddlAuto
	 */
	public void setDdlAuto(String ddlAuto) {
		this.ddlAuto = ddlAuto;
	}

	/**
	 * Returns the naming strategy used by the HibernateProperties.
	 * @return the naming strategy used by the HibernateProperties
	 */
	public Naming getNaming() {
		return this.naming;
	}

	/**
	 * Determine the configuration properties for the initialization of the main Hibernate
	 * EntityManagerFactory based on standard JPA properties and
	 * {@link HibernateSettings}.
	 * @param jpaProperties standard JPA properties
	 * @param settings the settings to apply when determining the configuration properties
	 * @return the Hibernate properties to use
	 */
	public Map<String, Object> determineHibernateProperties(Map<String, String> jpaProperties,
			HibernateSettings settings) {
		Assert.notNull(jpaProperties, "JpaProperties must not be null");
		Assert.notNull(settings, "Settings must not be null");
		return getAdditionalProperties(jpaProperties, settings);
	}

	/**
	 * Retrieves additional properties to be added to the existing properties map.
	 * @param existing the existing properties map
	 * @param settings the Hibernate settings
	 * @return a map of additional properties
	 */
	private Map<String, Object> getAdditionalProperties(Map<String, String> existing, HibernateSettings settings) {
		Map<String, Object> result = new HashMap<>(existing);
		applyScanner(result);
		getNaming().applyNamingStrategies(result);
		String ddlAuto = determineDdlAuto(existing, settings::getDdlAuto);
		if (StringUtils.hasText(ddlAuto) && !"none".equals(ddlAuto)) {
			result.put(AvailableSettings.HBM2DDL_AUTO, ddlAuto);
		}
		else {
			result.remove(AvailableSettings.HBM2DDL_AUTO);
		}
		Collection<HibernatePropertiesCustomizer> customizers = settings.getHibernatePropertiesCustomizers();
		if (!ObjectUtils.isEmpty(customizers)) {
			customizers.forEach((customizer) -> customizer.customize(result));
		}
		return result;
	}

	/**
	 * Applies the scanner to the given result map. If the result map does not contain the
	 * scanner key and the disabled scanner class is present, the disabled scanner class
	 * is added to the result map.
	 * @param result the result map to apply the scanner to
	 */
	private void applyScanner(Map<String, Object> result) {
		if (!result.containsKey(AvailableSettings.SCANNER) && ClassUtils.isPresent(DISABLED_SCANNER_CLASS, null)) {
			result.put(AvailableSettings.SCANNER, DISABLED_SCANNER_CLASS);
		}
	}

	/**
	 * Determines the value of the "hibernate.hbm2ddl.auto" property based on the existing
	 * configuration and default value.
	 * @param existing the existing configuration properties
	 * @param defaultDdlAuto a supplier for the default value of "hibernate.hbm2ddl.auto"
	 * @return the determined value of "hibernate.hbm2ddl.auto"
	 */
	private String determineDdlAuto(Map<String, String> existing, Supplier<String> defaultDdlAuto) {
		String ddlAuto = existing.get(AvailableSettings.HBM2DDL_AUTO);
		if (ddlAuto != null) {
			return ddlAuto;
		}
		if (this.ddlAuto != null) {
			return this.ddlAuto;
		}
		if (existing.get(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION) != null) {
			return null;
		}
		return defaultDdlAuto.get();
	}

	/**
	 * Naming class.
	 */
	public static class Naming {

		/**
		 * Fully qualified name of the implicit naming strategy.
		 */
		private String implicitStrategy;

		/**
		 * Fully qualified name of the physical naming strategy.
		 */
		private String physicalStrategy;

		/**
		 * Returns the implicit strategy used by the Naming class.
		 * @return the implicit strategy used by the Naming class
		 */
		public String getImplicitStrategy() {
			return this.implicitStrategy;
		}

		/**
		 * Sets the implicit strategy for the Naming class.
		 * @param implicitStrategy the implicit strategy to be set
		 */
		public void setImplicitStrategy(String implicitStrategy) {
			this.implicitStrategy = implicitStrategy;
		}

		/**
		 * Returns the physical strategy used by the Naming class.
		 * @return the physical strategy used by the Naming class
		 */
		public String getPhysicalStrategy() {
			return this.physicalStrategy;
		}

		/**
		 * Sets the physical strategy for the Naming class.
		 * @param physicalStrategy the physical strategy to be set
		 */
		public void setPhysicalStrategy(String physicalStrategy) {
			this.physicalStrategy = physicalStrategy;
		}

		/**
		 * Applies the naming strategies to the given properties map.
		 * @param properties the properties map to apply the naming strategies to
		 */
		private void applyNamingStrategies(Map<String, Object> properties) {
			applyNamingStrategy(properties, AvailableSettings.IMPLICIT_NAMING_STRATEGY, this.implicitStrategy,
					() -> SpringImplicitNamingStrategy.class.getName());
			applyNamingStrategy(properties, AvailableSettings.PHYSICAL_NAMING_STRATEGY, this.physicalStrategy,
					() -> CamelCaseToUnderscoresNamingStrategy.class.getName());
		}

		/**
		 * Applies a naming strategy to the given properties map.
		 * @param properties the properties map to apply the naming strategy to
		 * @param key the key to associate with the naming strategy in the properties map
		 * @param strategy the naming strategy to apply
		 * @param defaultStrategy a supplier function that provides a default naming
		 * strategy if the given strategy is null
		 */
		private void applyNamingStrategy(Map<String, Object> properties, String key, Object strategy,
				Supplier<String> defaultStrategy) {
			if (strategy != null) {
				properties.put(key, strategy);
			}
			else {
				properties.computeIfAbsent(key, (k) -> defaultStrategy.get());
			}
		}

	}

}

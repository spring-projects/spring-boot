/*
 * Copyright 2012-2019 the original author or authors.
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

import org.hibernate.cfg.AvailableSettings;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Hibernate.
 *
 * @author Stephane Nicoll
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
	 * Whether to use Hibernate's newer IdentifierGenerator for AUTO, TABLE and SEQUENCE.
	 * This is actually a shortcut for the "hibernate.id.new_generator_mappings" property.
	 * When not specified will default to "true".
	 */
	private Boolean useNewIdGeneratorMappings;

	public String getDdlAuto() {
		return this.ddlAuto;
	}

	public void setDdlAuto(String ddlAuto) {
		this.ddlAuto = ddlAuto;
	}

	public Boolean isUseNewIdGeneratorMappings() {
		return this.useNewIdGeneratorMappings;
	}

	public void setUseNewIdGeneratorMappings(Boolean useNewIdGeneratorMappings) {
		this.useNewIdGeneratorMappings = useNewIdGeneratorMappings;
	}

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

	private Map<String, Object> getAdditionalProperties(Map<String, String> existing, HibernateSettings settings) {
		Map<String, Object> result = new HashMap<>(existing);
		applyNewIdGeneratorMappings(result);
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

	private void applyNewIdGeneratorMappings(Map<String, Object> result) {
		if (this.useNewIdGeneratorMappings != null) {
			result.put(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, this.useNewIdGeneratorMappings.toString());
		}
		else if (!result.containsKey(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS)) {
			result.put(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true");
		}
	}

	private void applyScanner(Map<String, Object> result) {
		if (!result.containsKey(AvailableSettings.SCANNER) && ClassUtils.isPresent(DISABLED_SCANNER_CLASS, null)) {
			result.put(AvailableSettings.SCANNER, DISABLED_SCANNER_CLASS);
		}
	}

	private String determineDdlAuto(Map<String, String> existing, Supplier<String> defaultDdlAuto) {
		String ddlAuto = existing.get(AvailableSettings.HBM2DDL_AUTO);
		if (ddlAuto != null) {
			return ddlAuto;
		}
		return (this.ddlAuto != null) ? this.ddlAuto : defaultDdlAuto.get();
	}

	public static class Naming {

		/**
		 * Fully qualified name of the implicit naming strategy.
		 */
		private String implicitStrategy;

		/**
		 * Fully qualified name of the physical naming strategy.
		 */
		private String physicalStrategy;

		public String getImplicitStrategy() {
			return this.implicitStrategy;
		}

		public void setImplicitStrategy(String implicitStrategy) {
			this.implicitStrategy = implicitStrategy;
		}

		public String getPhysicalStrategy() {
			return this.physicalStrategy;
		}

		public void setPhysicalStrategy(String physicalStrategy) {
			this.physicalStrategy = physicalStrategy;
		}

		private void applyNamingStrategies(Map<String, Object> properties) {
			applyNamingStrategy(properties, AvailableSettings.IMPLICIT_NAMING_STRATEGY, this.implicitStrategy,
					SpringImplicitNamingStrategy.class.getName());
			applyNamingStrategy(properties, AvailableSettings.PHYSICAL_NAMING_STRATEGY, this.physicalStrategy,
					SpringPhysicalNamingStrategy.class.getName());
		}

		private void applyNamingStrategy(Map<String, Object> properties, String key, Object strategy,
				Object defaultStrategy) {
			if (strategy != null) {
				properties.put(key, strategy);
			}
			else if (defaultStrategy != null && !properties.containsKey(key)) {
				properties.put(key, defaultStrategy);
			}
		}

	}

}

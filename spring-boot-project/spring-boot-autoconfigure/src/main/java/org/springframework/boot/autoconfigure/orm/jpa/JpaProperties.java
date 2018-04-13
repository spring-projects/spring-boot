/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.orm.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * External configuration properties for a JPA EntityManagerFactory created by Spring.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Madhura Bhave
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.jpa")
public class JpaProperties {

	/**
	 * Additional native properties to set on the JPA provider.
	 */
	private Map<String, String> properties = new HashMap<>();

	/**
	 * Mapping resources (equivalent to "mapping-file" entries in persistence.xml).
	 */
	private final List<String> mappingResources = new ArrayList<>();

	/**
	 * Name of the target database to operate on, auto-detected by default. Can be
	 * alternatively set using the "Database" enum.
	 */
	private String databasePlatform;

	/**
	 * Target database to operate on, auto-detected by default. Can be alternatively set
	 * using the "databasePlatform" property.
	 */
	private Database database;

	/**
	 * Whether to initialize the schema on startup.
	 */
	private boolean generateDdl = false;

	/**
	 * Whether to enable logging of SQL statements.
	 */
	private boolean showSql = false;

	/**
	 * Register OpenEntityManagerInViewInterceptor. Binds a JPA EntityManager to the
	 * thread for the entire processing of the request.
	 */
	private Boolean openInView;

	private Hibernate hibernate = new Hibernate();

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public List<String> getMappingResources() {
		return this.mappingResources;
	}

	public String getDatabasePlatform() {
		return this.databasePlatform;
	}

	public void setDatabasePlatform(String databasePlatform) {
		this.databasePlatform = databasePlatform;
	}

	public Database getDatabase() {
		return this.database;
	}

	public void setDatabase(Database database) {
		this.database = database;
	}

	public boolean isGenerateDdl() {
		return this.generateDdl;
	}

	public void setGenerateDdl(boolean generateDdl) {
		this.generateDdl = generateDdl;
	}

	public boolean isShowSql() {
		return this.showSql;
	}

	public void setShowSql(boolean showSql) {
		this.showSql = showSql;
	}

	public Boolean getOpenInView() {
		return this.openInView;
	}

	public void setOpenInView(Boolean openInView) {
		this.openInView = openInView;
	}

	public Hibernate getHibernate() {
		return this.hibernate;
	}

	public void setHibernate(Hibernate hibernate) {
		this.hibernate = hibernate;
	}

	/**
	 * Get configuration properties for the initialization of the main Hibernate
	 * EntityManagerFactory.
	 * @param settings the settings to apply when determining the configuration properties
	 * @return some Hibernate properties for configuration
	 */
	public Map<String, Object> getHibernateProperties(HibernateSettings settings) {
		return this.hibernate.getAdditionalProperties(this.properties, settings);
	}

	/**
	 * Determine the {@link Database} to use based on this configuration and the primary
	 * {@link DataSource}.
	 * @param dataSource the auto-configured data source
	 * @return {@code Database}
	 */
	public Database determineDatabase(DataSource dataSource) {
		if (this.database != null) {
			return this.database;
		}
		return DatabaseLookup.getDatabase(dataSource);
	}

	public static class Hibernate {

		private static final String USE_NEW_ID_GENERATOR_MAPPINGS = "hibernate.id."
				+ "new_generator_mappings";

		/**
		 * DDL mode. This is actually a shortcut for the "hibernate.hbm2ddl.auto"
		 * property. Defaults to "create-drop" when using an embedded database and no
		 * schema manager was detected. Otherwise, defaults to "none".
		 */
		private String ddlAuto;

		/**
		 * Whether to use Hibernate's newer IdentifierGenerator for AUTO, TABLE and
		 * SEQUENCE. This is actually a shortcut for the
		 * "hibernate.id.new_generator_mappings" property. When not specified will default
		 * to "true".
		 */
		private Boolean useNewIdGeneratorMappings;

		private final Naming naming = new Naming();

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

		private Map<String, Object> getAdditionalProperties(Map<String, String> existing,
				HibernateSettings settings) {
			Map<String, Object> result = new HashMap<>(existing);
			applyNewIdGeneratorMappings(result);
			getNaming().applyNamingStrategies(result,
					settings.getImplicitNamingStrategy(),
					settings.getPhysicalNamingStrategy());
			String ddlAuto = determineDdlAuto(existing, settings::getDdlAuto);
			if (StringUtils.hasText(ddlAuto) && !"none".equals(ddlAuto)) {
				result.put("hibernate.hbm2ddl.auto", ddlAuto);
			}
			else {
				result.remove("hibernate.hbm2ddl.auto");
			}
			Collection<HibernatePropertiesCustomizer> customizers = settings
					.getHibernatePropertiesCustomizers();
			if (!ObjectUtils.isEmpty(customizers)) {
				customizers.forEach((customizer) -> customizer.customize(result));
			}
			return result;
		}

		private void applyNewIdGeneratorMappings(Map<String, Object> result) {
			if (this.useNewIdGeneratorMappings != null) {
				result.put(USE_NEW_ID_GENERATOR_MAPPINGS,
						this.useNewIdGeneratorMappings.toString());
			}
			else if (!result.containsKey(USE_NEW_ID_GENERATOR_MAPPINGS)) {
				result.put(USE_NEW_ID_GENERATOR_MAPPINGS, "true");
			}
		}

		private String determineDdlAuto(Map<String, String> existing,
				Supplier<String> defaultDdlAuto) {
			if (!existing.containsKey("hibernate.hbm2ddl.auto")) {
				String ddlAuto = (this.ddlAuto != null ? this.ddlAuto
						: defaultDdlAuto.get());
				if (!"none".equals(ddlAuto)) {
					return ddlAuto;
				}
			}
			if (existing.containsKey("hibernate.hbm2ddl.auto")) {
				return existing.get("hibernate.hbm2ddl.auto");
			}
			return "none";
		}

	}

	public static class Naming {

		private static final String DEFAULT_PHYSICAL_STRATEGY = "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy";

		private static final String DEFAULT_IMPLICIT_STRATEGY = "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy";

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

		private void applyNamingStrategies(Map<String, Object> properties,
				ImplicitNamingStrategy implicitStrategyBean,
				PhysicalNamingStrategy physicalStrategyBean) {
			applyNamingStrategy(properties, "hibernate.implicit_naming_strategy",
					implicitStrategyBean != null ? implicitStrategyBean
							: this.implicitStrategy,
					DEFAULT_IMPLICIT_STRATEGY);
			applyNamingStrategy(properties, "hibernate.physical_naming_strategy",
					physicalStrategyBean != null ? physicalStrategyBean
							: this.physicalStrategy,
					DEFAULT_PHYSICAL_STRATEGY);
		}

		private void applyNamingStrategy(Map<String, Object> properties, String key,
				Object strategy, Object defaultStrategy) {
			if (strategy != null) {
				properties.put(key, strategy);
			}
			else if (defaultStrategy != null && !properties.containsKey(key)) {
				properties.put(key, defaultStrategy);
			}
		}

	}

}

/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.util.StringUtils;

/**
 * External configuration properties for a JPA EntityManagerFactory created by Spring.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.jpa")
public class JpaProperties {

	/**
	 * Additional native properties to set on the JPA provider.
	 */
	private Map<String, String> properties = new HashMap<String, String>();

	/**
	 * Name of the target database to operate on, auto-detected by default. Can be
	 * alternatively set using the "Database" enum.
	 */
	private String databasePlatform;

	/**
	 * Target database to operate on, auto-detected by default. Can be alternatively set
	 * using the "databasePlatform" property.
	 */
	private Database database = Database.DEFAULT;

	/**
	 * Initialize the schema on startup.
	 */
	private boolean generateDdl = false;

	/**
	 * Enable logging of SQL statements.
	 */
	private boolean showSql = false;

	private Hibernate hibernate = new Hibernate();

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
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

	public Hibernate getHibernate() {
		return this.hibernate;
	}

	public void setHibernate(Hibernate hibernate) {
		this.hibernate = hibernate;
	}

	/**
	 * Get configuration properties for the initialization of the main Hibernate
	 * EntityManagerFactory.
	 * @param dataSource the DataSource in case it is needed to determine the properties
	 * @return some Hibernate properties for configuration
	 */
	public Map<String, String> getHibernateProperties(DataSource dataSource) {
		return this.hibernate.getAdditionalProperties(this.properties, dataSource);
	}

	public static class Hibernate {

		private static final String USE_NEW_ID_GENERATOR_MAPPINGS = "hibernate.id."
				+ "new_generator_mappings";

		/**
		 * DDL mode. This is actually a shortcut for the "hibernate.hbm2ddl.auto"
		 * property. Default to "create-drop" when using an embedded database, "none"
		 * otherwise.
		 */
		private String ddlAuto;

		/**
		 * Use Hibernate's newer IdentifierGenerator for AUTO, TABLE and SEQUENCE. This is
		 * actually a shortcut for the "hibernate.id.new_generator_mappings" property.
		 * When not specified will default to "false" with Hibernate 5 for back
		 * compatibility.
		 */
		private Boolean useNewIdGeneratorMappings;

		@NestedConfigurationProperty
		private final Naming naming = new Naming();

		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "spring.jpa.hibernate.naming.strategy")
		public String getNamingStrategy() {
			return getNaming().getStrategy();
		}

		@Deprecated
		public void setNamingStrategy(String namingStrategy) {
			getNaming().setStrategy(namingStrategy);
		}

		public String getDdlAuto() {
			return this.ddlAuto;
		}

		public void setDdlAuto(String ddlAuto) {
			this.ddlAuto = ddlAuto;
		}

		public boolean isUseNewIdGeneratorMappings() {
			return this.useNewIdGeneratorMappings;
		}

		public void setUseNewIdGeneratorMappings(boolean useNewIdGeneratorMappings) {
			this.useNewIdGeneratorMappings = useNewIdGeneratorMappings;
		}

		public Naming getNaming() {
			return this.naming;
		}

		private Map<String, String> getAdditionalProperties(Map<String, String> existing,
				DataSource dataSource) {
			Map<String, String> result = new HashMap<String, String>(existing);
			applyNewIdGeneratorMappings(result);
			getNaming().applyNamingStrategy(result);
			String ddlAuto = getOrDeduceDdlAuto(existing, dataSource);
			if (StringUtils.hasText(ddlAuto) && !"none".equals(ddlAuto)) {
				result.put("hibernate.hbm2ddl.auto", ddlAuto);
			}
			else {
				result.remove("hibernate.hbm2ddl.auto");
			}
			return result;
		}

		private void applyNewIdGeneratorMappings(Map<String, String> result) {
			if (this.useNewIdGeneratorMappings != null) {
				result.put(USE_NEW_ID_GENERATOR_MAPPINGS,
						this.useNewIdGeneratorMappings.toString());
			}
			else if (HibernateVersion.getRunning() == HibernateVersion.V5
					&& !result.containsKey(USE_NEW_ID_GENERATOR_MAPPINGS)) {
				result.put(USE_NEW_ID_GENERATOR_MAPPINGS, "false");
			}
		}

		private String getOrDeduceDdlAuto(Map<String, String> existing,
				DataSource dataSource) {
			String ddlAuto = (this.ddlAuto != null ? this.ddlAuto
					: getDefaultDdlAuto(dataSource));
			if (!existing.containsKey("hibernate." + "hbm2ddl.auto")
					&& !"none".equals(ddlAuto)) {
				return ddlAuto;
			}
			if (existing.containsKey("hibernate." + "hbm2ddl.auto")) {
				return existing.get("hibernate.hbm2ddl.auto");
			}
			return "none";
		}

		private String getDefaultDdlAuto(DataSource dataSource) {
			if (EmbeddedDatabaseConnection.isEmbedded(dataSource)) {
				return "create-drop";
			}
			return "none";
		}

	}

	public static class Naming {

		private static final String DEFAULT_HIBERNATE4_STRATEGY = "org.springframework.boot.orm.jpa.hibernate.SpringNamingStrategy";

		private static final String DEFAULT_PHYSICAL_STRATEGY = "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy";

		private static final String DEFAULT_IMPLICIT_STRATEGY = "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy";

		/**
		 * Hibernate 5 implicit naming strategy fully qualified name.
		 */
		private String implicitStrategy;

		/**
		 * Hibernate 5 physical naming strategy fully qualified name.
		 */
		private String physicalStrategy;

		/**
		 * Hibernate 4 naming strategy fully qualified name. Not supported with Hibernate
		 * 5.
		 */
		private String strategy;

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

		public String getStrategy() {
			return this.strategy;
		}

		public void setStrategy(String strategy) {
			this.strategy = strategy;
		}

		private void applyNamingStrategy(Map<String, String> properties) {
			switch (HibernateVersion.getRunning()) {
			case V4:
				applyHibernate4NamingStrategy(properties);
				break;
			case V5:
				applyHibernate5NamingStrategy(properties);
				break;
			}
		}

		private void applyHibernate5NamingStrategy(Map<String, String> properties) {
			applyHibernate5NamingStrategy(properties,
					"hibernate.implicit_naming_strategy", this.implicitStrategy,
					DEFAULT_IMPLICIT_STRATEGY);
			applyHibernate5NamingStrategy(properties,
					"hibernate.physical_naming_strategy", this.physicalStrategy,
					DEFAULT_PHYSICAL_STRATEGY);
		}

		private void applyHibernate5NamingStrategy(Map<String, String> properties,
				String key, String strategy, String defaultStrategy) {
			if (strategy != null) {
				properties.put(key, strategy);
			}
			else if (defaultStrategy != null && !properties.containsKey(key)) {
				properties.put(key, defaultStrategy);
			}
		}

		private void applyHibernate4NamingStrategy(Map<String, String> properties) {
			if (!properties.containsKey("hibernate.ejb.naming_strategy_delegator")) {
				properties.put("hibernate.ejb.naming_strategy",
						getHibernate4NamingStrategy(properties));
			}
		}

		private String getHibernate4NamingStrategy(Map<String, String> existing) {
			if (!existing.containsKey("hibernate.ejb.naming_strategy")
					&& this.strategy != null) {
				return this.strategy;
			}
			return DEFAULT_HIBERNATE4_STRATEGY;
		}

	}

}

/*
 * Copyright 2012-2014 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.SpringNamingStrategy;
import org.springframework.orm.jpa.vendor.Database;

/**
 * External configuration properties for a JPA EntityManagerFactory created by Spring.
 * 
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.jpa")
public class JpaProperties {

	private static final Log logger = LogFactory.getLog(JpaProperties.class);

	private Map<String, Object> properties = new HashMap<String, Object>();

	private String databasePlatform;

	private Database database = Database.DEFAULT;

	private boolean generateDdl = false;

	private boolean showSql = false;

	private Hibernate hibernate = new Hibernate();

	public Map<String, Object> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, Object> properties) {
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
	 * EntityManagerFactory. The result will always have ddl-auto=none, so that the schema
	 * generation or validation can be deferred to a later stage.
	 * @param dataSource the DataSource in case it is needed to determine the properties
	 * @return some Hibernate properties for configuration
	 */
	public Map<String, Object> getInitialHibernateProperties(DataSource dataSource) {
		return this.hibernate.getAdditionalProperties(this.properties);
	}

	/**
	 * Get the full configuration properties for the Hibernate EntityManagerFactory.
	 * @param dataSource the DataSource in case it is needed to determine the properties
	 * @return some Hibernate properties for configuration
	 */
	public Map<String, Object> getHibernateProperties(DataSource dataSource) {
		return this.hibernate
				.getDeferredAdditionalProperties(this.properties, dataSource);
	}

	public static class Hibernate {

		private Class<?> namingStrategy;

		private static Class<?> DEFAULT_NAMING_STRATEGY = SpringNamingStrategy.class;

		private String ddlAuto;

		private boolean deferDdl = true;

		public Class<?> getNamingStrategy() {
			return this.namingStrategy;
		}

		public void setNamingStrategy(Class<?> namingStrategy) {
			this.namingStrategy = namingStrategy;
		}

		@Deprecated
		public void setNamingstrategy(Class<?> namingStrategy) {
			logger.warn("The property spring.jpa.namingstrategy has been renamed, "
					+ "please update your configuration to use nameing-strategy");
			this.setNamingStrategy(namingStrategy);
		}

		public String getDdlAuto() {
			return this.ddlAuto;
		}

		public void setDeferDdl(boolean deferDdl) {
			this.deferDdl = deferDdl;
		}

		public boolean isDeferDdl() {
			return this.deferDdl;
		}

		private String getDeferredDdlAuto(Map<String, Object> existing,
				DataSource dataSource) {
			if (!this.deferDdl) {
				return "none";
			}
			String ddlAuto = this.ddlAuto != null ? this.ddlAuto
					: getDefaultDdlAuto(dataSource);
			if (!isAlreadyProvided(existing, "hbm2ddl.auto") && !"none".equals(ddlAuto)) {
				return ddlAuto;
			}
			if (isAlreadyProvided(existing, "hbm2ddl.auto")) {
				return (String) existing.get("hibernate.hbm2ddl.auto");
			}
			return "none";
		}

		public void setDdlAuto(String ddlAuto) {
			this.ddlAuto = ddlAuto;
		}

		private Map<String, Object> getDeferredAdditionalProperties(
				Map<String, Object> properties, DataSource dataSource) {
			Map<String, Object> deferred = getAdditionalProperties(properties);
			deferred.put("hibernate.hbm2ddl.auto",
					getDeferredDdlAuto(properties, dataSource));
			return deferred;
		}

		private Map<String, Object> getAdditionalProperties(Map<String, Object> existing) {
			Map<String, Object> result = new HashMap<String, Object>();
			if (!isAlreadyProvided(existing, "ejb.naming_strategy")
					&& this.namingStrategy != null) {
				result.put("hibernate.ejb.naming_strategy", this.namingStrategy.getName());
			}
			else if (this.namingStrategy == null) {
				result.put("hibernate.ejb.naming_strategy",
						DEFAULT_NAMING_STRATEGY.getName());
			}
			if (this.deferDdl) {
				result.put("hibernate.hbm2ddl.auto", "none");
			}
			else {
				result.put("hibernate.hbm2ddl.auto", this.ddlAuto);
			}
			return result;
		}

		private boolean isAlreadyProvided(Map<String, Object> existing, String key) {
			return existing.containsKey("hibernate." + key);
		}

		private String getDefaultDdlAuto(DataSource dataSource) {
			if (EmbeddedDatabaseConnection.isEmbedded(dataSource)) {
				return "create-drop";
			}
			return "none";
		}

	}

}

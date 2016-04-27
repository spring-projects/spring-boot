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

package org.springframework.boot.autoconfigure.liquibase;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import liquibase.servicelocator.ServiceLocator;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.liquibase.CommonsLoggingLiquibaseLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Liquibase.
 *
 * @author Marcel Overdijk
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass(SpringLiquibase.class)
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "liquibase", name = "enabled", matchIfMissing = true)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class })
public class LiquibaseAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(SpringLiquibase.class)
	@EnableConfigurationProperties(LiquibaseProperties.class)
	@Import(LiquibaseJpaDependencyConfiguration.class)
	public static class LiquibaseConfiguration {

		private final LiquibaseProperties properties;

		private final ResourceLoader resourceLoader;

		private final DataSource dataSource;

		public LiquibaseConfiguration(LiquibaseProperties properties,
				ResourceLoader resourceLoader, DataSource dataSource) {
			this.properties = properties;
			this.resourceLoader = resourceLoader;
			this.dataSource = dataSource;
		}

		@PostConstruct
		public void checkChangelogExists() {
			if (this.properties.isCheckChangeLogLocation()) {
				Resource resource = this.resourceLoader
						.getResource(this.properties.getChangeLog());
				Assert.state(resource.exists(),
						"Cannot find changelog location: " + resource
								+ " (please add changelog or check your Liquibase "
								+ "configuration)");
			}
			ServiceLocator serviceLocator = ServiceLocator.getInstance();
			serviceLocator.addPackageToScan(
					CommonsLoggingLiquibaseLogger.class.getPackage().getName());
		}

		@Bean
		public SpringLiquibase liquibase() {
			SpringLiquibase liquibase = new SpringLiquibase();
			liquibase.setChangeLog(this.properties.getChangeLog());
			liquibase.setContexts(this.properties.getContexts());
			liquibase.setDataSource(getDataSource());
			liquibase.setDefaultSchema(this.properties.getDefaultSchema());
			liquibase.setDropFirst(this.properties.isDropFirst());
			liquibase.setShouldRun(this.properties.isEnabled());
			liquibase.setLabels(this.properties.getLabels());
			liquibase.setChangeLogParameters(this.properties.getParameters());
			liquibase.setRollbackFile(this.properties.getRollbackFile());
			return liquibase;
		}

		private DataSource getDataSource() {
			if (this.properties.getUrl() == null) {
				return this.dataSource;
			}
			return DataSourceBuilder.create().url(this.properties.getUrl())
					.username(this.properties.getUser())
					.password(this.properties.getPassword()).build();
		}
	}

	/**
	 * Additional configuration to ensure that {@link EntityManagerFactory} beans
	 * depend-on the liquibase bean.
	 */
	@Configuration
	@ConditionalOnClass(LocalContainerEntityManagerFactoryBean.class)
	@ConditionalOnBean(AbstractEntityManagerFactoryBean.class)
	protected static class LiquibaseJpaDependencyConfiguration
			extends EntityManagerFactoryDependsOnPostProcessor {

		public LiquibaseJpaDependencyConfiguration() {
			super("liquibase");
		}

	}

}

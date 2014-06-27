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

package org.springframework.boot.autoconfigure.flyway;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Flyway database migrations.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnBean(DataSource.class)
@ConditionalOnExpression("${flyway.enabled:true}")
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class FlywayAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(Flyway.class)
	@EnableConfigurationProperties(FlywayProperties.class)
	@Import(FlywayJpaDependencyConfiguration.class)
	public static class FlywayConfiguration {

		@Autowired
		private FlywayProperties properties = new FlywayProperties();

		@Autowired
		private ResourceLoader resourceLoader = new DefaultResourceLoader();

		@Autowired(required = false)
		private DataSource dataSource;

		@Autowired(required = false)
		@FlywayDataSource
		private DataSource flywayDataSource;

		@PostConstruct
		public void checkLocationExists() {
			if (this.properties.isCheckLocation()) {
				Assert.state(!this.properties.getLocations().isEmpty(),
						"Migration script locations not configured");
				boolean exists = hasAtLeastOneLocation();
				Assert.state(exists, "Cannot find migrations location in: "
						+ this.properties.getLocations()
						+ " (please add migrations or check your Flyway configuration)");
			}
		}

		private boolean hasAtLeastOneLocation() {
			for (String location : this.properties.getLocations()) {
				if (this.resourceLoader.getResource(location).exists()) {
					return true;
				}
			}
			return false;
		}

		@Bean(initMethod = "migrate")
		@ConfigurationProperties(prefix = "flyway")
		public Flyway flyway() {
			Flyway flyway = new Flyway();
			if (this.properties.isCreateDataSource()) {
				flyway.setDataSource(this.properties.getUrl(), this.properties.getUser(),
						this.properties.getPassword(), this.properties.getInitSqls()
								.toArray(new String[0]));
			}
			else if (this.flywayDataSource != null) {
				flyway.setDataSource(this.flywayDataSource);
			}
			else {
				flyway.setDataSource(this.dataSource);
			}
			return flyway;
		}

	}

	/**
	 * Additional configuration to ensure that {@link EntityManagerFactory} beans
	 * depend-on the flyway bean.
	 */
	@Configuration
	@ConditionalOnClass(LocalContainerEntityManagerFactoryBean.class)
	@ConditionalOnBean(AbstractEntityManagerFactoryBean.class)
	protected static class FlywayJpaDependencyConfiguration extends
			EntityManagerFactoryDependsOnPostProcessor {

		public FlywayJpaDependencyConfiguration() {
			super("flyway");
		}

	}

}

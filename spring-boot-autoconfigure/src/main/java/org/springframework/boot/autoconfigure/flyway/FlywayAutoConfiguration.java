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
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Flyway database migrations.
 * 
 * @author Dave Syer
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
	public static class FlywayConfiguration {

		@Autowired
		private FlywayProperties properties = new FlywayProperties();

		@Autowired
		private ResourceLoader resourceLoader = new DefaultResourceLoader();

		@Autowired
		private DataSource dataSource;

		@PostConstruct
		public void checkLocationExists() {
			if (this.properties.isCheckLocation()) {

				Assert.state(!this.properties.getLocations().isEmpty(),
						"Migration script locations not configured");
				boolean exists = false;
				for (String location : this.properties.getLocations()) {
					Resource resource = this.resourceLoader.getResource(location);
					exists = (!exists && resource.exists());
				}
				Assert.state(exists, "Cannot find migrations location in: "
						+ this.properties.getLocations()
						+ " (please add migrations or check your Flyway configuration)");
			}
		}

		@Bean
		public Flyway flyway(DataSource dataSource) {
			Flyway flyway = new Flyway();
			flyway.setLocations(this.properties.getLocations().toArray(new String[0]));
			flyway.setSchemas(this.properties.getSchemas().toArray(new String[0]));
			flyway.setInitVersion(this.properties.getInitVersion());
			flyway.setSqlMigrationPrefix(this.properties.getPrefix());
			flyway.setSqlMigrationSuffix(this.properties.getSuffix());
			flyway.setDataSource(dataSource);
			flyway.migrate();
			return flyway;
		}

	}

}

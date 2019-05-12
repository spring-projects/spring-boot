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

package org.springframework.boot.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.support.JmxUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a JNDI located
 * {@link DataSource}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.2.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore({ XADataSourceAutoConfiguration.class,
		DataSourceAutoConfiguration.class })
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@ConditionalOnProperty(prefix = "spring.datasource", name = "jndi-name")
@EnableConfigurationProperties(DataSourceProperties.class)
public class JndiDataSourceAutoConfiguration {

	@Bean(destroyMethod = "")
	@ConditionalOnMissingBean
	public DataSource dataSource(DataSourceProperties properties,
			ApplicationContext context) {
		JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
		DataSource dataSource = dataSourceLookup.getDataSource(properties.getJndiName());
		excludeMBeanIfNecessary(dataSource, "dataSource", context);
		return dataSource;
	}

	private void excludeMBeanIfNecessary(Object candidate, String beanName,
			ApplicationContext context) {
		for (MBeanExporter mbeanExporter : context.getBeansOfType(MBeanExporter.class)
				.values()) {
			if (JmxUtils.isMBean(candidate.getClass())) {
				mbeanExporter.addExcludedBean(beanName);
			}
		}
	}

}

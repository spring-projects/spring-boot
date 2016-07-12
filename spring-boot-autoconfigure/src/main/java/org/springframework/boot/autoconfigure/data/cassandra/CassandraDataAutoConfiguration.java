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

package org.springframework.boot.autoconfigure.data.cassandra;

import java.util.List;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.data.cassandra.config.CassandraEntityClassScanner;
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.convert.CassandraConverter;
import org.springframework.data.cassandra.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.mapping.BasicCassandraMappingContext;
import org.springframework.data.cassandra.mapping.CassandraMappingContext;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Cassandra support.
 *
 * @author Julien Dubois
 * @author Eddú Meléndez
 * @author Mark Paluch
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass({ Cluster.class, CassandraAdminOperations.class })
@EnableConfigurationProperties(CassandraProperties.class)
@AutoConfigureAfter(CassandraAutoConfiguration.class)
public class CassandraDataAutoConfiguration {

	private final BeanFactory beanFactory;

	private final CassandraProperties properties;

	private final Cluster cluster;

	private final PropertyResolver propertyResolver;

	public CassandraDataAutoConfiguration(BeanFactory beanFactory,
			CassandraProperties properties, Cluster cluster, Environment environment) {
		this.beanFactory = beanFactory;
		this.properties = properties;
		this.cluster = cluster;
		this.propertyResolver = new RelaxedPropertyResolver(environment,
				"spring.data.cassandra.");
	}

	@Bean
	@ConditionalOnMissingBean
	public CassandraMappingContext cassandraMapping() throws ClassNotFoundException {
		BasicCassandraMappingContext context = new BasicCassandraMappingContext();
		List<String> packages = EntityScanPackages.get(this.beanFactory)
				.getPackageNames();
		if (packages.isEmpty() && AutoConfigurationPackages.has(this.beanFactory)) {
			packages = AutoConfigurationPackages.get(this.beanFactory);
		}
		if (!packages.isEmpty()) {
			context.setInitialEntitySet(CassandraEntityClassScanner.scan(packages));
		}
		return context;
	}

	@Bean
	@ConditionalOnMissingBean
	public CassandraConverter cassandraConverter(CassandraMappingContext mapping) {
		return new MappingCassandraConverter(mapping);
	}

	@Bean
	@ConditionalOnMissingBean(Session.class)
	public CassandraSessionFactoryBean session(CassandraConverter converter)
			throws Exception {
		CassandraSessionFactoryBean session = new CassandraSessionFactoryBean();
		session.setCluster(this.cluster);
		session.setConverter(converter);
		session.setKeyspaceName(this.properties.getKeyspaceName());
		SchemaAction schemaAction = this.propertyResolver.getProperty("schemaAction",
				SchemaAction.class, SchemaAction.NONE);
		session.setSchemaAction(schemaAction);
		return session;
	}

	@Bean
	@ConditionalOnMissingBean
	public CassandraTemplate cassandraTemplate(Session session,
			CassandraConverter converter) throws Exception {
		return new CassandraTemplate(session, converter);
	}

}

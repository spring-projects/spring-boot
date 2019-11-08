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

package org.springframework.boot.autoconfigure.data.cassandra;

import java.util.Collections;
import java.util.List;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.cassandra.config.CassandraEntityClassScanner;
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.convert.CassandraCustomConversions;
import org.springframework.data.cassandra.core.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.core.mapping.SimpleUserTypeResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Cassandra support.
 *
 * @author Julien Dubois
 * @author Eddú Meléndez
 * @author Mark Paluch
 * @author Madhura Bhave
 * @since 1.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Cluster.class, CassandraAdminOperations.class })
@ConditionalOnBean(Cluster.class)
@EnableConfigurationProperties(CassandraProperties.class)
@AutoConfigureAfter(CassandraAutoConfiguration.class)
public class CassandraDataAutoConfiguration {

	private final CassandraProperties properties;

	private final Cluster cluster;

	public CassandraDataAutoConfiguration(CassandraProperties properties, Cluster cluster) {
		this.properties = properties;
		this.cluster = cluster;
	}

	@Bean
	@ConditionalOnMissingBean
	public CassandraMappingContext cassandraMapping(BeanFactory beanFactory, CassandraCustomConversions conversions)
			throws ClassNotFoundException {
		CassandraMappingContext context = new CassandraMappingContext();
		List<String> packages = EntityScanPackages.get(beanFactory).getPackageNames();
		if (packages.isEmpty() && AutoConfigurationPackages.has(beanFactory)) {
			packages = AutoConfigurationPackages.get(beanFactory);
		}
		if (!packages.isEmpty()) {
			context.setInitialEntitySet(CassandraEntityClassScanner.scan(packages));
		}
		PropertyMapper.get().from(this.properties::getKeyspaceName).whenHasText().as(this::createSimpleUserTypeResolver)
				.to(context::setUserTypeResolver);
		context.setCustomConversions(conversions);
		return context;
	}

	private SimpleUserTypeResolver createSimpleUserTypeResolver(String keyspaceName) {
		return new SimpleUserTypeResolver(this.cluster, keyspaceName);
	}

	@Bean
	@ConditionalOnMissingBean
	public CassandraConverter cassandraConverter(CassandraMappingContext mapping,
			CassandraCustomConversions conversions) {
		MappingCassandraConverter converter = new MappingCassandraConverter(mapping);
		converter.setCustomConversions(conversions);
		return converter;
	}

	@Bean
	@ConditionalOnMissingBean(Session.class)
	public CassandraSessionFactoryBean cassandraSession(Environment environment, CassandraConverter converter) {
		CassandraSessionFactoryBean session = new CassandraSessionFactoryBean();
		session.setCluster(this.cluster);
		session.setConverter(converter);
		session.setKeyspaceName(this.properties.getKeyspaceName());
		Binder binder = Binder.get(environment);
		binder.bind("spring.data.cassandra.schema-action", SchemaAction.class).ifBound(session::setSchemaAction);
		return session;
	}

	@Bean
	@ConditionalOnMissingBean(CassandraOperations.class)
	public CassandraTemplate cassandraTemplate(Session session, CassandraConverter converter) {
		return new CassandraTemplate(session, converter);
	}

	@Bean
	@ConditionalOnMissingBean
	public CassandraCustomConversions cassandraCustomConversions() {
		return new CassandraCustomConversions(Collections.emptyList());
	}

}

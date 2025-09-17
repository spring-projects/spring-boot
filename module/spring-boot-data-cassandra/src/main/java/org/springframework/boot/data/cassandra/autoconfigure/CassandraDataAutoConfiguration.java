/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.cassandra.autoconfigure;

import java.util.Collections;
import java.util.List;

import com.datastax.oss.driver.api.core.CqlSession;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.cassandra.autoconfigure.CassandraAutoConfiguration;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.persistence.autoconfigure.EntityScanPackages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.cassandra.CassandraManagedTypes;
import org.springframework.data.cassandra.SessionFactory;
import org.springframework.data.cassandra.config.CassandraEntityClassScanner;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.config.SessionFactoryFactoryBean;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.convert.CassandraCustomConversions;
import org.springframework.data.cassandra.core.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.cql.CqlOperations;
import org.springframework.data.cassandra.core.cql.CqlTemplate;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.core.mapping.SimpleUserTypeResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Cassandra support.
 *
 * @author Julien Dubois
 * @author Eddú Meléndez
 * @author Mark Paluch
 * @author Madhura Bhave
 * @author Christoph Strobl
 * @since 4.0.0
 */
@AutoConfiguration(after = CassandraAutoConfiguration.class)
@ConditionalOnClass({ CqlSession.class, CassandraAdminOperations.class })
@ConditionalOnBean(CqlSession.class)
public final class CassandraDataAutoConfiguration {

	private final CqlSession session;

	CassandraDataAutoConfiguration(@Lazy CqlSession session) {
		this.session = session;
	}

	@Bean
	@ConditionalOnMissingBean
	static CassandraManagedTypes cassandraManagedTypes(BeanFactory beanFactory) throws ClassNotFoundException {
		List<String> packages = EntityScanPackages.get(beanFactory).getPackageNames();
		if (packages.isEmpty() && AutoConfigurationPackages.has(beanFactory)) {
			packages = AutoConfigurationPackages.get(beanFactory);
		}
		if (!packages.isEmpty()) {
			return CassandraManagedTypes.fromIterable(CassandraEntityClassScanner.scan(packages));
		}
		return CassandraManagedTypes.empty();
	}

	@Bean
	@ConditionalOnMissingBean
	CassandraMappingContext cassandraMappingContext(CassandraManagedTypes cassandraManagedTypes,
			CassandraCustomConversions conversions) {
		CassandraMappingContext context = new CassandraMappingContext();
		context.setManagedTypes(cassandraManagedTypes);
		context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
		return context;
	}

	@Bean
	@ConditionalOnMissingBean
	CassandraConverter cassandraConverter(CassandraMappingContext mapping, CassandraCustomConversions conversions) {
		MappingCassandraConverter converter = new MappingCassandraConverter(mapping);
		converter.setCodecRegistry(() -> this.session.getContext().getCodecRegistry());
		converter.setCustomConversions(conversions);
		converter.setUserTypeResolver(new SimpleUserTypeResolver(this.session));
		return converter;
	}

	@Bean
	@ConditionalOnMissingBean(SessionFactory.class)
	SessionFactoryFactoryBean cassandraSessionFactory(Environment environment, CassandraConverter converter) {
		SessionFactoryFactoryBean session = new SessionFactoryFactoryBean();
		session.setSession(this.session);
		session.setConverter(converter);
		Binder binder = Binder.get(environment);
		binder.bind("spring.cassandra.schema-action", SchemaAction.class).ifBound(session::setSchemaAction);
		return session;
	}

	@Bean
	@ConditionalOnMissingBean(CqlOperations.class)
	CqlTemplate cqlTemplate(SessionFactory sessionFactory) {
		return new CqlTemplate(sessionFactory);
	}

	@Bean
	@ConditionalOnMissingBean(CassandraOperations.class)
	CassandraTemplate cassandraTemplate(CqlTemplate cqlTemplate, CassandraConverter converter) {
		return new CassandraTemplate(cqlTemplate, converter);
	}

	@Bean
	@ConditionalOnMissingBean
	CassandraCustomConversions cassandraCustomConversions() {
		return new CassandraCustomConversions(Collections.emptyList());
	}

}

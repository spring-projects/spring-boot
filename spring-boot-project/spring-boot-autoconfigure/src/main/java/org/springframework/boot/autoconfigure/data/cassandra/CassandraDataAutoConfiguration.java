/*
 * Copyright 2012-2022 the original author or authors.
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

import com.datastax.oss.driver.api.core.CqlSession;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
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
@AutoConfiguration(after = CassandraAutoConfiguration.class)
@ConditionalOnClass({ CqlSession.class, CassandraAdminOperations.class })
@ConditionalOnBean(CqlSession.class)
public class CassandraDataAutoConfiguration {

	private final CqlSession session;

	/**
     * Constructs a new instance of the CassandraDataAutoConfiguration class with the specified CqlSession.
     * 
     * @param session the CqlSession to be used for interacting with Cassandra.
     */
    public CassandraDataAutoConfiguration(CqlSession session) {
		this.session = session;
	}

	/**
     * Returns the CassandraManagedTypes bean if it is not already present in the application context.
     * This bean is responsible for scanning the packages for Cassandra entity classes and returning the managed types.
     * If no packages are specified, it will scan the packages from the EntityScanPackages and AutoConfigurationPackages.
     * If no packages are found, it will return an empty CassandraManagedTypes object.
     *
     * @param beanFactory the BeanFactory used to retrieve the package names
     * @return the CassandraManagedTypes bean
     * @throws ClassNotFoundException if the entity classes cannot be found
     */
    @Bean
	@ConditionalOnMissingBean
	public static CassandraManagedTypes cassandraManagedTypes(BeanFactory beanFactory) throws ClassNotFoundException {
		List<String> packages = EntityScanPackages.get(beanFactory).getPackageNames();
		if (packages.isEmpty() && AutoConfigurationPackages.has(beanFactory)) {
			packages = AutoConfigurationPackages.get(beanFactory);
		}
		if (!packages.isEmpty()) {
			return CassandraManagedTypes.fromIterable(CassandraEntityClassScanner.scan(packages));
		}
		return CassandraManagedTypes.empty();
	}

	/**
     * Creates and configures a {@link CassandraMappingContext} bean if no other bean of the same type is present.
     * 
     * @param cassandraManagedTypes The managed types for the Cassandra mapping context.
     * @param conversions The custom conversions for the Cassandra mapping context.
     * @return The configured {@link CassandraMappingContext} bean.
     */
    @Bean
	@ConditionalOnMissingBean
	public CassandraMappingContext cassandraMappingContext(CassandraManagedTypes cassandraManagedTypes,
			CassandraCustomConversions conversions) {
		CassandraMappingContext context = new CassandraMappingContext();
		context.setManagedTypes(cassandraManagedTypes);
		context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
		return context;
	}

	/**
     * Creates a {@link CassandraConverter} bean if no other bean of the same type is present.
     * 
     * @param mapping the {@link CassandraMappingContext} used for mapping entities to Cassandra tables
     * @param conversions the {@link CassandraCustomConversions} used for custom type conversions
     * @return the created {@link CassandraConverter} bean
     */
    @Bean
	@ConditionalOnMissingBean
	public CassandraConverter cassandraConverter(CassandraMappingContext mapping,
			CassandraCustomConversions conversions) {
		MappingCassandraConverter converter = new MappingCassandraConverter(mapping);
		converter.setCodecRegistry(this.session.getContext().getCodecRegistry());
		converter.setCustomConversions(conversions);
		converter.setUserTypeResolver(new SimpleUserTypeResolver(this.session));
		return converter;
	}

	/**
     * Creates a Cassandra Session Factory if no other bean of type SessionFactory is present.
     * 
     * @param environment the environment object
     * @param converter the Cassandra converter
     * @return the SessionFactoryFactoryBean
     */
    @Bean
	@ConditionalOnMissingBean(SessionFactory.class)
	public SessionFactoryFactoryBean cassandraSessionFactory(Environment environment, CassandraConverter converter) {
		SessionFactoryFactoryBean session = new SessionFactoryFactoryBean();
		session.setSession(this.session);
		session.setConverter(converter);
		Binder binder = Binder.get(environment);
		binder.bind("spring.cassandra.schema-action", SchemaAction.class).ifBound(session::setSchemaAction);
		return session;
	}

	/**
     * Creates a new instance of CassandraTemplate if there is no existing bean of type CassandraOperations.
     * 
     * @param sessionFactory the SessionFactory used for creating Cassandra sessions
     * @param converter the CassandraConverter used for converting between Java objects and Cassandra entities
     * @return a new instance of CassandraTemplate
     */
    @Bean
	@ConditionalOnMissingBean(CassandraOperations.class)
	public CassandraTemplate cassandraTemplate(SessionFactory sessionFactory, CassandraConverter converter) {
		return new CassandraTemplate(sessionFactory, converter);
	}

	/**
     * Creates a new instance of {@link CassandraCustomConversions} if no other bean of the same type is present.
     * 
     * @return the {@link CassandraCustomConversions} bean
     */
    @Bean
	@ConditionalOnMissingBean
	public CassandraCustomConversions cassandraCustomConversions() {
		return new CassandraCustomConversions(Collections.emptyList());
	}

}

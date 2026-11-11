/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.cassandra.autoconfigure;

import com.datastax.oss.driver.api.core.CqlSession;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.cassandra.ReactiveSession;
import org.springframework.data.cassandra.ReactiveSessionFactory;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.cql.ReactiveCqlOperations;
import org.springframework.data.cassandra.core.cql.ReactiveCqlTemplate;
import org.springframework.data.cassandra.core.cql.session.DefaultBridgedReactiveSession;
import org.springframework.data.cassandra.core.cql.session.DefaultReactiveSessionFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's reactive Cassandra
 * support.
 *
 * @author Eddú Meléndez
 * @author Mark Paluch
 * @since 4.0.0
 */
@AutoConfiguration(after = DataCassandraAutoConfiguration.class)
@ConditionalOnClass({ CqlSession.class, ReactiveCassandraTemplate.class, Flux.class })
@ConditionalOnBean(CqlSession.class)
public final class DataCassandraReactiveAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ReactiveSession reactiveCassandraSession(CqlSession session) {
		return new DefaultBridgedReactiveSession(session);
	}

	@Bean
	@ConditionalOnMissingBean
	ReactiveSessionFactory reactiveCassandraSessionFactory(ReactiveSession reactiveCassandraSession) {
		return new DefaultReactiveSessionFactory(reactiveCassandraSession);
	}

	@Bean
	@ConditionalOnMissingBean(ReactiveCqlOperations.class)
	ReactiveCqlTemplate reactiveCqlTemplate(ReactiveSessionFactory reactiveCassandraSessionFactory) {
		return new ReactiveCqlTemplate(reactiveCassandraSessionFactory);
	}

	@Bean
	@ConditionalOnMissingBean(ReactiveCassandraOperations.class)
	ReactiveCassandraTemplate reactiveCassandraTemplate(ReactiveCqlTemplate reactiveCqlTemplate,
			CassandraConverter converter) {
		return new ReactiveCassandraTemplate(reactiveCqlTemplate, converter);
	}

}

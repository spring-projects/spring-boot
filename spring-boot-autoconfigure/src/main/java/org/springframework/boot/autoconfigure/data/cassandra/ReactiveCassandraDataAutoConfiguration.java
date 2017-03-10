/*
 * Copyright 2012-2017 the original author or authors.
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

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import reactor.core.scheduler.Schedulers;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cassandra.core.session.DefaultBridgedReactiveSession;
import org.springframework.cassandra.core.session.DefaultReactiveSessionFactory;
import org.springframework.cassandra.core.session.ReactiveSession;
import org.springframework.cassandra.core.session.ReactiveSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.convert.CassandraConverter;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's reactive Cassandra
 * support.
 *
 * @author Eddú Meléndez
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass({ Cluster.class, ReactiveCassandraTemplate.class })
@ConditionalOnBean(Session.class)
@AutoConfigureAfter(CassandraAutoConfiguration.class)
public class ReactiveCassandraDataAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ReactiveSession.class)
	public ReactiveSession rectiveSession(Session session)
			throws Exception {
		return new DefaultBridgedReactiveSession(session, Schedulers.elastic());
	}

	@Bean
	public ReactiveSessionFactory reactiveSessionFactory(ReactiveSession reactiveSession)
			throws Exception {
		return new DefaultReactiveSessionFactory(reactiveSession);
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactiveCassandraTemplate reactiveCassandraTemplate(ReactiveSession session,
			CassandraConverter converter) throws Exception {
		return new ReactiveCassandraTemplate(session, converter);
	}

}

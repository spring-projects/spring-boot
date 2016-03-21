/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.neo4j;

import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Neo4j support.
 * <p>
 * Registers a {@link Neo4jTemplate} bean if no other bean of
 * the same type is configured.
 *
 * @author Michael Hunger
 * @author Josh Long
 * @author Vince Bickers
 * @since 1.3.0
 */
@Configuration
@EnableConfigurationProperties(Neo4jProperties.class)
@ConditionalOnMissingBean(type = "org.springframework.data.neo4j.template.Neo4jOperations")
@ConditionalOnClass({ Neo4jSession.class, Neo4jOperations.class })
public class Neo4jAutoConfiguration extends Neo4jConfiguration {

	@Autowired
	private Neo4jProperties properties;

	@Value("${spring.data.neo4j.domain.packages:null}")
	private String[] domainPackages;

	@Bean
	@ConditionalOnMissingBean(org.neo4j.ogm.config.Configuration.class)
	public org.neo4j.ogm.config.Configuration configuration() {
		return this.properties.configure();
	}

	@Override
	@ConditionalOnMissingBean(SessionFactory.class)
	public SessionFactory getSessionFactory() {
		return new SessionFactory(configuration(), this.domainPackages);
	}

	@Bean
	@ConditionalOnMissingBean(Session.class)
	@Scope(value = "${spring.data.neo4j.session.lifetime:session}", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public Session getSession() throws Exception {
		return getSessionFactory().openSession();
	}

}

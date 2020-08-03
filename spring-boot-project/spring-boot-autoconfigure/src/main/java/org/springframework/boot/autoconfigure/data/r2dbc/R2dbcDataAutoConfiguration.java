/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.r2dbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.core.DefaultReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.r2dbc.core.DatabaseClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link DatabaseClient}.
 *
 * @author Mark Paluch
 * @author Oliver Drotbohm
 * @since 2.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ DatabaseClient.class, R2dbcEntityTemplate.class })
@ConditionalOnSingleCandidate(DatabaseClient.class)
@AutoConfigureAfter(R2dbcAutoConfiguration.class)
public class R2dbcDataAutoConfiguration {

	private final DatabaseClient databaseClient;

	public R2dbcDataAutoConfiguration(DatabaseClient databaseClient) {
		this.databaseClient = databaseClient;
	}

	@Bean
	@ConditionalOnMissingBean
	public R2dbcEntityTemplate r2dbcEntityTemplate(ReactiveDataAccessStrategy reactiveDataAccessStrategy) {
		return new R2dbcEntityTemplate(this.databaseClient, reactiveDataAccessStrategy);
	}

	@Bean
	@ConditionalOnMissingBean
	public R2dbcMappingContext r2dbcMappingContext(ObjectProvider<NamingStrategy> namingStrategy,
			R2dbcCustomConversions r2dbcCustomConversions) {
		R2dbcMappingContext relationalMappingContext = new R2dbcMappingContext(
				namingStrategy.getIfAvailable(() -> NamingStrategy.INSTANCE));
		relationalMappingContext.setSimpleTypeHolder(r2dbcCustomConversions.getSimpleTypeHolder());
		return relationalMappingContext;
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactiveDataAccessStrategy reactiveDataAccessStrategy(R2dbcMappingContext mappingContext,
			R2dbcCustomConversions r2dbcCustomConversions) {
		MappingR2dbcConverter converter = new MappingR2dbcConverter(mappingContext, r2dbcCustomConversions);
		return new DefaultReactiveDataAccessStrategy(getDialect(), converter);
	}

	@Bean
	@ConditionalOnMissingBean
	public R2dbcCustomConversions r2dbcCustomConversions() {
		R2dbcDialect dialect = getDialect();
		List<Object> converters = new ArrayList<>(dialect.getConverters());
		converters.addAll(R2dbcCustomConversions.STORE_CONVERTERS);
		return new R2dbcCustomConversions(
				CustomConversions.StoreConversions.of(dialect.getSimpleTypeHolder(), converters),
				Collections.emptyList());
	}

	private R2dbcDialect getDialect() {
		return DialectResolver.getDialect(this.databaseClient.getConnectionFactory());
	}

}

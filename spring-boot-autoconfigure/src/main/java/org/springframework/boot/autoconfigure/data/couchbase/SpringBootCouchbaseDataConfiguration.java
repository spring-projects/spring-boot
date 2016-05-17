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

package org.springframework.boot.autoconfigure.data.couchbase;

import java.util.Collections;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseDataConfiguration;
import org.springframework.data.couchbase.config.BeanNames;
import org.springframework.data.couchbase.config.CouchbaseConfigurer;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.couchbase.core.convert.CustomConversions;
import org.springframework.data.couchbase.core.convert.MappingCouchbaseConverter;
import org.springframework.data.couchbase.core.mapping.CouchbaseMappingContext;
import org.springframework.data.couchbase.core.query.Consistency;
import org.springframework.data.couchbase.repository.support.IndexManager;

/**
 * Configure Spring Data's couchbase support.
 *
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnMissingBean(AbstractCouchbaseDataConfiguration.class)
@ConditionalOnBean(CouchbaseConfigurer.class)
class SpringBootCouchbaseDataConfiguration extends AbstractCouchbaseDataConfiguration {

	private final CouchbaseDataProperties properties;

	private final CouchbaseConfigurer couchbaseConfigurer;

	private final BeanFactory beanFactory;

	SpringBootCouchbaseDataConfiguration(CouchbaseDataProperties properties,
			ObjectProvider<CouchbaseConfigurer> couchbaseConfigurerProvider, BeanFactory factory) {
		this.properties = properties;
		this.couchbaseConfigurer = couchbaseConfigurerProvider.getIfAvailable();
		this.beanFactory = factory;
	}

	@Override
	protected CouchbaseConfigurer couchbaseConfigurer() {
		return this.couchbaseConfigurer;
	}

	@Override
	protected Consistency getDefaultConsistency() {
		return this.properties.getConsistency();
	}

	@Override
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_TEMPLATE)
	@Bean(name = BeanNames.COUCHBASE_TEMPLATE)
	public CouchbaseTemplate couchbaseTemplate() throws Exception {
		return super.couchbaseTemplate();
	}

	@Override
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_INDEX_MANAGER)
	@Bean(name = BeanNames.COUCHBASE_INDEX_MANAGER)
	public IndexManager indexManager() {
		if (this.properties.isAutoIndex()) {
			return new IndexManager(true, true, true);
		}
		return new IndexManager(false, false, false);
	}

	@Override
	public String typeKey() {
		return this.properties.getTypeKey();
	}

	@Override
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_CUSTOM_CONVERSIONS)
	@Bean(name = BeanNames.COUCHBASE_CUSTOM_CONVERSIONS)
	public CustomConversions customConversions() {
		return new CustomConversions(Collections.emptyList());
	}

	//this method uses customeConversions() in the parent class, needs to rely on bean
	@Override
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_MAPPING_CONVERTER)
	@Bean(name = BeanNames.COUCHBASE_MAPPING_CONVERTER)
	public MappingCouchbaseConverter mappingCouchbaseConverter() throws Exception {
		CouchbaseMappingContext mappingContextBean = beanFactory.getBean(BeanNames.COUCHBASE_MAPPING_CONTEXT, CouchbaseMappingContext.class);
		CustomConversions conversionsBean = beanFactory.getBean(BeanNames.COUCHBASE_CUSTOM_CONVERSIONS, CustomConversions.class);

		MappingCouchbaseConverter converter = new MappingCouchbaseConverter(mappingContextBean, typeKey());
		converter.setCustomConversions(conversionsBean);
		return converter;
	}

	//this method uses customeConversions() in the parent class, needs to rely on bean
	@Override
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_MAPPING_CONTEXT)
	@Bean(name = BeanNames.COUCHBASE_MAPPING_CONTEXT)
	public CouchbaseMappingContext couchbaseMappingContext() throws Exception {
		CustomConversions conversionsBean = beanFactory.getBean(BeanNames.COUCHBASE_CUSTOM_CONVERSIONS, CustomConversions.class);

		CouchbaseMappingContext mappingContext = new CouchbaseMappingContext();
		mappingContext.setInitialEntitySet(getInitialEntitySet());
		mappingContext.setSimpleTypeHolder(conversionsBean.getSimpleTypeHolder());
		mappingContext.setFieldNamingStrategy(fieldNamingStrategy());
		return mappingContext;
	}

}

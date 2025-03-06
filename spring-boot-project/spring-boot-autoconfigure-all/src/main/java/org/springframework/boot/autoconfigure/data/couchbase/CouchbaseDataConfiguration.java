/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.couchbase;

import java.util.Collections;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.BeanNames;
import org.springframework.data.couchbase.core.convert.CouchbaseCustomConversions;
import org.springframework.data.couchbase.core.convert.MappingCouchbaseConverter;
import org.springframework.data.couchbase.core.convert.translation.JacksonTranslationService;
import org.springframework.data.couchbase.core.convert.translation.TranslationService;
import org.springframework.data.couchbase.core.mapping.CouchbaseMappingContext;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.mapping.model.FieldNamingStrategy;

/**
 * Configuration for Spring Data's couchbase support.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
class CouchbaseDataConfiguration {

	@Bean
	@ConditionalOnMissingBean
	MappingCouchbaseConverter couchbaseMappingConverter(CouchbaseDataProperties properties,
			CouchbaseMappingContext couchbaseMappingContext, CouchbaseCustomConversions couchbaseCustomConversions) {
		MappingCouchbaseConverter converter = new MappingCouchbaseConverter(couchbaseMappingContext,
				properties.getTypeKey());
		converter.setCustomConversions(couchbaseCustomConversions);
		return converter;
	}

	@Bean
	@ConditionalOnMissingBean
	TranslationService couchbaseTranslationService() {
		return new JacksonTranslationService();
	}

	@Bean(name = BeanNames.COUCHBASE_MAPPING_CONTEXT)
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_MAPPING_CONTEXT)
	CouchbaseMappingContext couchbaseMappingContext(CouchbaseDataProperties properties,
			ApplicationContext applicationContext, CouchbaseCustomConversions couchbaseCustomConversions)
			throws ClassNotFoundException {
		CouchbaseMappingContext mappingContext = new CouchbaseMappingContext();
		mappingContext.setInitialEntitySet(new EntityScanner(applicationContext).scan(Document.class));
		mappingContext.setSimpleTypeHolder(couchbaseCustomConversions.getSimpleTypeHolder());
		Class<?> fieldNamingStrategy = properties.getFieldNamingStrategy();
		if (fieldNamingStrategy != null) {
			mappingContext
				.setFieldNamingStrategy((FieldNamingStrategy) BeanUtils.instantiateClass(fieldNamingStrategy));
		}
		mappingContext.setAutoIndexCreation(properties.isAutoIndex());
		return mappingContext;
	}

	@Bean(name = BeanNames.COUCHBASE_CUSTOM_CONVERSIONS)
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_CUSTOM_CONVERSIONS)
	CouchbaseCustomConversions couchbaseCustomConversions() {
		return new CouchbaseCustomConversions(Collections.emptyList());
	}

}

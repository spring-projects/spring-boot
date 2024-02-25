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

	/**
	 * Creates a MappingCouchbaseConverter bean if no other bean of the same type is
	 * present.
	 * @param properties The CouchbaseDataProperties object containing the configuration
	 * properties.
	 * @param couchbaseMappingContext The CouchbaseMappingContext object used for mapping
	 * entities to Couchbase documents.
	 * @param couchbaseCustomConversions The CouchbaseCustomConversions object containing
	 * custom type conversions.
	 * @return The MappingCouchbaseConverter bean.
	 */
	@Bean
	@ConditionalOnMissingBean
	MappingCouchbaseConverter couchbaseMappingConverter(CouchbaseDataProperties properties,
			CouchbaseMappingContext couchbaseMappingContext, CouchbaseCustomConversions couchbaseCustomConversions) {
		MappingCouchbaseConverter converter = new MappingCouchbaseConverter(couchbaseMappingContext,
				properties.getTypeKey());
		converter.setCustomConversions(couchbaseCustomConversions);
		return converter;
	}

	/**
	 * Returns a new instance of the TranslationService interface if no other bean of the
	 * same type is present. This method creates and returns a JacksonTranslationService
	 * object, which is an implementation of the TranslationService interface.
	 * @return a new instance of the TranslationService interface if no other bean of the
	 * same type is present
	 */
	@Bean
	@ConditionalOnMissingBean
	TranslationService couchbaseTranslationService() {
		return new JacksonTranslationService();
	}

	/**
	 * Creates and configures a {@link CouchbaseMappingContext} bean.
	 *
	 * This method is annotated with {@link Bean} and {@link ConditionalOnMissingBean} to
	 * ensure that it is only executed if no other bean with the same name exists in the
	 * application context.
	 *
	 * The {@link CouchbaseMappingContext} is responsible for mapping entities to
	 * Couchbase documents.
	 *
	 * The method takes the following parameters: - {@link CouchbaseDataProperties
	 * properties}: The properties for configuring Couchbase data. -
	 * {@link ApplicationContext applicationContext}: The application context. -
	 * {@link CouchbaseCustomConversions couchbaseCustomConversions}: The custom
	 * conversions for Couchbase data.
	 *
	 * The method first creates a new instance of {@link CouchbaseMappingContext}.
	 *
	 * It then sets the initial entity set by scanning for entities annotated with
	 * {@link Document} using the {@link EntityScanner} class and the provided application
	 * context.
	 *
	 * The simple type holder is set using the custom conversions.
	 *
	 * If a field naming strategy is specified in the properties, it is instantiated and
	 * set on the mapping context.
	 *
	 * The auto index creation flag is also set on the mapping context.
	 *
	 * Finally, the created mapping context is returned.
	 * @param properties The properties for configuring Couchbase data.
	 * @param applicationContext The application context.
	 * @param couchbaseCustomConversions The custom conversions for Couchbase data.
	 * @return The configured {@link CouchbaseMappingContext} bean.
	 * @throws ClassNotFoundException If the field naming strategy class cannot be found.
	 */
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

	/**
	 * Creates a new instance of {@link CouchbaseCustomConversions} if no bean with the
	 * name {@link BeanNames#COUCHBASE_CUSTOM_CONVERSIONS} is already present in the
	 * application context.
	 *
	 * This method is annotated with {@link ConditionalOnMissingBean} to ensure that it is
	 * only executed if no bean with the specified name is already defined.
	 *
	 * The created {@link CouchbaseCustomConversions} instance is initialized with an
	 * empty list of converters.
	 * @return a new instance of {@link CouchbaseCustomConversions} with an empty list of
	 * converters
	 */
	@Bean(name = BeanNames.COUCHBASE_CUSTOM_CONVERSIONS)
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_CUSTOM_CONVERSIONS)
	CouchbaseCustomConversions couchbaseCustomConversions() {
		return new CouchbaseCustomConversions(Collections.emptyList());
	}

}

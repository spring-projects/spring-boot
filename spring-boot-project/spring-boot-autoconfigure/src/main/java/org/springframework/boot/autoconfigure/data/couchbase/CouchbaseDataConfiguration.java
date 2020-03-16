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

package org.springframework.boot.autoconfigure.data.couchbase;

import java.util.Collections;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.cluster.ClusterInfo;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.couchbase.config.BeanNames;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.couchbase.core.convert.CouchbaseCustomConversions;
import org.springframework.data.couchbase.core.convert.MappingCouchbaseConverter;
import org.springframework.data.couchbase.core.convert.translation.JacksonTranslationService;
import org.springframework.data.couchbase.core.convert.translation.TranslationService;
import org.springframework.data.couchbase.core.mapping.CouchbaseMappingContext;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.config.RepositoryOperationsMapping;
import org.springframework.data.couchbase.repository.support.IndexManager;
import org.springframework.data.mapping.model.FieldNamingStrategy;

/**
 * Configuration for Spring Data's couchbase support.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
class CouchbaseDataConfiguration {

	@Bean(name = BeanNames.COUCHBASE_MAPPING_CONVERTER)
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_MAPPING_CONVERTER)
	MappingCouchbaseConverter couchbaseMappingConverter(CouchbaseDataProperties properties,
			CouchbaseMappingContext couchbaseMappingContext, CouchbaseCustomConversions couchbaseCustomConversions) {
		MappingCouchbaseConverter converter = new MappingCouchbaseConverter(couchbaseMappingContext,
				properties.getTypeKey());
		converter.setCustomConversions(couchbaseCustomConversions);
		return converter;
	}

	@Bean(name = BeanNames.COUCHBASE_TRANSLATION_SERVICE)
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_TRANSLATION_SERVICE)
	TranslationService couchbaseTranslationService() {
		return new JacksonTranslationService();
	}

	@Bean(name = BeanNames.COUCHBASE_MAPPING_CONTEXT)
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_MAPPING_CONTEXT)
	CouchbaseMappingContext couchbaseMappingContext(CouchbaseDataProperties properties,
			ApplicationContext applicationContext, CouchbaseCustomConversions couchbaseCustomConversions)
			throws Exception {
		CouchbaseMappingContext mappingContext = new CouchbaseMappingContext();
		mappingContext
				.setInitialEntitySet(new EntityScanner(applicationContext).scan(Document.class, Persistent.class));
		mappingContext.setSimpleTypeHolder(couchbaseCustomConversions.getSimpleTypeHolder());
		Class<?> fieldNamingStrategy = properties.getFieldNamingStrategy();
		if (fieldNamingStrategy != null) {
			mappingContext
					.setFieldNamingStrategy((FieldNamingStrategy) BeanUtils.instantiateClass(fieldNamingStrategy));
		}
		return mappingContext;
	}

	@Bean(name = BeanNames.COUCHBASE_CUSTOM_CONVERSIONS)
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_CUSTOM_CONVERSIONS)
	CouchbaseCustomConversions couchbaseCustomConversions() {
		return new CouchbaseCustomConversions(Collections.emptyList());
	}

	@Bean(name = BeanNames.COUCHBASE_INDEX_MANAGER)
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_INDEX_MANAGER)
	IndexManager indexManager(CouchbaseDataProperties properties) {
		if (properties.isAutoIndex()) {
			return new IndexManager(true, true, true);
		}
		return new IndexManager(false, false, false);
	}

	@Bean(name = BeanNames.COUCHBASE_TEMPLATE)
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_TEMPLATE)
	@ConditionalOnBean({ ClusterInfo.class, Bucket.class })
	CouchbaseTemplate couchbaseTemplate(CouchbaseDataProperties properties, ClusterInfo clusterInfo, Bucket bucket,
			MappingCouchbaseConverter mappingCouchbaseConverter, TranslationService translationService) {
		CouchbaseTemplate template = new CouchbaseTemplate(clusterInfo, bucket, mappingCouchbaseConverter,
				translationService);
		template.setDefaultConsistency(properties.getConsistency());
		return template;
	}

	@Bean(name = BeanNames.COUCHBASE_OPERATIONS_MAPPING)
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_OPERATIONS_MAPPING)
	@ConditionalOnSingleCandidate(CouchbaseTemplate.class)
	RepositoryOperationsMapping repositoryOperationsMapping(CouchbaseTemplate couchbaseTemplate) {
		return new RepositoryOperationsMapping(couchbaseTemplate);
	}

}

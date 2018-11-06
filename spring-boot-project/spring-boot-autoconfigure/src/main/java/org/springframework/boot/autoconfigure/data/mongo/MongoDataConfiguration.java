/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.boot.autoconfigure.data.mongo;

import java.util.Collections;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * Base configuration class for Spring Data's mongo support.
 *
 * @author Madhura Bhave
 */
@Configuration
class MongoDataConfiguration {

	private final ApplicationContext applicationContext;

	private final MongoProperties properties;

	MongoDataConfiguration(ApplicationContext applicationContext,
			MongoProperties properties) {
		this.applicationContext = applicationContext;
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public MongoMappingContext mongoMappingContext(MongoCustomConversions conversions)
			throws ClassNotFoundException {
		MongoMappingContext context = new MongoMappingContext();
		context.setInitialEntitySet(new EntityScanner(this.applicationContext)
				.scan(Document.class, Persistent.class));
		Class<?> strategyClass = this.properties.getFieldNamingStrategy();
		if (strategyClass != null) {
			context.setFieldNamingStrategy(
					(FieldNamingStrategy) BeanUtils.instantiateClass(strategyClass));
		}
		context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
		return context;
	}

	@Bean
	@ConditionalOnMissingBean
	public MongoCustomConversions mongoCustomConversions() {
		return new MongoCustomConversions(Collections.emptyList());
	}

}

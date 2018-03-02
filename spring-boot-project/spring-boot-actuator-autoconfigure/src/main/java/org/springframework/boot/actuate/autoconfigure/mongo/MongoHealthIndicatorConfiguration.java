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

package org.springframework.boot.actuate.autoconfigure.mongo;

import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthIndicatorConfiguration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.mongo.MongoHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Configuration for {@link MongoHealthIndicator}.
 *
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnClass(MongoTemplate.class)
@ConditionalOnBean(MongoTemplate.class)
class MongoHealthIndicatorConfiguration extends
		CompositeHealthIndicatorConfiguration<MongoHealthIndicator, MongoTemplate> {

	private final Map<String, MongoTemplate> mongoTemplates;

	MongoHealthIndicatorConfiguration(Map<String, MongoTemplate> mongoTemplates) {
		this.mongoTemplates = mongoTemplates;
	}

	@Bean
	@ConditionalOnMissingBean(name = "mongoHealthIndicator")
	public HealthIndicator mongoHealthIndicator() {
		return createHealthIndicator(this.mongoTemplates);
	}

}

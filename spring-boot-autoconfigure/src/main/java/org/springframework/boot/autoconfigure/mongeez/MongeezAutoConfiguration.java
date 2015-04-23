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

package org.springframework.boot.autoconfigure.mongeez;

import com.mongodb.Mongo;
import org.mongeez.Mongeez;
import org.mongeez.MongeezRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Mongo.
 *
 * @author Eddú Meléndez
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass(Mongeez.class)
@ConditionalOnBean(Mongo.class)
@AutoConfigureAfter(MongoAutoConfiguration.class)
@EnableConfigurationProperties(MongeezProperties.class)
public class MongeezAutoConfiguration {

	@Autowired
	private MongeezProperties mongeezProperties;

	@Bean
	@ConditionalOnMissingBean
	@ConfigurationProperties(prefix = "mongeez")
	public MongeezRunner mongeezRunner(Mongo mongo) {
		MongeezRunner mongeez = new MongeezRunner();
		mongeez.setMongo(mongo);
		mongeez.setFile(new ClassPathResource(this.mongeezProperties.getChangeLog()));
		return mongeez;
	}

}

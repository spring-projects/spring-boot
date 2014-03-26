/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.mongo;

import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.Mongo;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's
 * {@link MongoTemplate}.
 * <p>
 * Registers a {@link org.springframework.data.mongodb.core.MongoTemplate} bean if no
 * other bean of the same type is configured.
 * <P>
 * Honors the {@literal spring.data.mongodb.database} property if set, otherwise connects
 * to the {@literal test} database.
 * 
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Josh Long
 */
@Configuration
@ConditionalOnClass({ Mongo.class, MongoTemplate.class })
public class MongoTemplateAutoConfiguration {

	@Autowired
	private MongoProperties properties;

	@Bean
	@ConditionalOnMissingBean
	public MongoTemplate mongoTemplate(Mongo mongo) throws UnknownHostException {
		return new MongoTemplate(mongo, this.properties.getMongoClientDatabase());
	}

}

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
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.util.StringUtils;

import com.mongodb.Mongo;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's mongo support.
 * <p>
 * Registers a {@link MongoTemplate} and {@link GridFsTemplate} beans if no other beans of
 * the same type are configured.
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
@ConditionalOnBean(MongoProperties.class)
@AutoConfigureAfter(MongoAutoConfiguration.class)
public class MongoDataAutoConfiguration {

	@Autowired
	private MongoProperties properties;

	@Bean
	@ConditionalOnMissingBean
	public MongoDbFactory mongoDbFactory(Mongo mongo) throws Exception {
		String db = this.properties.getMongoClientDatabase();
		return new SimpleMongoDbFactory(mongo, db);
	}

	@Bean
	@ConditionalOnMissingBean
	public MongoTemplate mongoTemplate(MongoDbFactory mongoDbFactory)
			throws UnknownHostException {
		return new MongoTemplate(mongoDbFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	public GridFsTemplate gridFsTemplate(Mongo mongo, MongoTemplate mongoTemplate) {
		String db = StringUtils.hasText(this.properties.getGridFsDatabase()) ? this.properties
				.getGridFsDatabase() : this.properties.getMongoClientDatabase();
		return new GridFsTemplate(new SimpleMongoDbFactory(mongo, db),
				mongoTemplate.getConverter());
	}

}

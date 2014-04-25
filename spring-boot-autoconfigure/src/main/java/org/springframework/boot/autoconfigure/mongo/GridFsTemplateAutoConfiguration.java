/*
 * Copyright 2012-2013 the original author or authors.
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

import com.mongodb.Mongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.util.StringUtils;

/**
 * <P>
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for
 * Mongo's {@link com.mongodb.gridfs.GridFS Grid FS} Spring Data's
 * {@link org.springframework.data.mongodb.gridfs.GridFsTemplate GridFsTemplate}.
 *
 * <p>
 * You can override which GridFS database is used by specifying {@code spring.data.mongodb.gridFsDatabase},
 * otherwise it defaults to the general {@code spring.data.mongodb.database} parameter.
 *
 * @author Josh Long
 */
@Configuration
@ConditionalOnClass({Mongo.class, GridFsTemplate.class})
@EnableConfigurationProperties(MongoProperties.class)
@AutoConfigureAfter(MongoTemplateAutoConfiguration.class)
public class GridFsTemplateAutoConfiguration {

    @Autowired
    private MongoProperties mongoProperties;

    @Bean
    @ConditionalOnMissingBean
    public MongoDbFactory mongoDbFactory(Mongo mongo) throws Exception {

        String db = StringUtils.hasText(this.mongoProperties.getGridFsDatabase()) ?
                this.mongoProperties.getGridFsDatabase() : this.mongoProperties.getMongoClientDatabase() ;

        return new SimpleMongoDbFactory(mongo, db );
    }

    @Bean
    @ConditionalOnMissingBean
    public GridFsTemplate gridFsTemplate(MongoDbFactory mongoDbFactory,
                                         MongoTemplate mongoTemplate) {
        return new GridFsTemplate(mongoDbFactory, mongoTemplate.getConverter());
    }


}

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

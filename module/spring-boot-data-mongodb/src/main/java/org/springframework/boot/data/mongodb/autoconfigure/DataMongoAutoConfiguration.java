/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.autoconfigure;

import com.mongodb.client.MongoClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.mongodb.autoconfigure.MongoProperties;
import org.springframework.boot.mongodb.autoconfigure.PropertiesMongoConnectionDetails;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's MongoDB support.
 * <p>
 * Registers a {@link MongoTemplate} and {@link GridFsTemplate} beans if no other beans of
 * the same type are configured.
 * <p>
 * Honors the {@literal spring.mongodb.database} property if set, otherwise connects to
 * the {@literal test} database.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Josh Long
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Christoph Strobl
 * @since 4.0.0
 */
@AutoConfiguration(after = MongoAutoConfiguration.class)
@ConditionalOnClass({ MongoClient.class, MongoTemplate.class })
@EnableConfigurationProperties({ MongoProperties.class, DataMongoProperties.class })
@Import({ DataMongoConfiguration.class, MongoDatabaseFactoryConfiguration.class,
		MongoDatabaseFactoryDependentConfiguration.class })
public final class DataMongoAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(MongoConnectionDetails.class)
	PropertiesMongoConnectionDetails mongoConnectionDetails(MongoProperties properties,
			ObjectProvider<SslBundles> sslBundles) {
		return new PropertiesMongoConnectionDetails(properties, sslBundles.getIfAvailable());
	}

}

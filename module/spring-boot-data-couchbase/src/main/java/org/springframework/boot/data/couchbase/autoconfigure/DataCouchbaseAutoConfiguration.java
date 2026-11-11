/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.autoconfigure;

import com.couchbase.client.java.Bucket;
import jakarta.validation.Validator;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.couchbase.core.mapping.event.ValidatingCouchbaseEventListener;
import org.springframework.data.couchbase.repository.CouchbaseRepository;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Couchbase support.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = CouchbaseAutoConfiguration.class,
		afterName = "org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration")
@ConditionalOnClass({ Bucket.class, CouchbaseRepository.class })
@EnableConfigurationProperties(DataCouchbaseProperties.class)
@Import({ DataCouchbaseConfiguration.class, CouchbaseClientFactoryConfiguration.class,
		CouchbaseClientFactoryDependentConfiguration.class })
public final class DataCouchbaseAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Validator.class)
	static class ValidationConfiguration {

		@Bean
		@ConditionalOnSingleCandidate(Validator.class)
		ValidatingCouchbaseEventListener validationEventListener(Validator validator) {
			return new ValidatingCouchbaseEventListener(validator);
		}

	}

}

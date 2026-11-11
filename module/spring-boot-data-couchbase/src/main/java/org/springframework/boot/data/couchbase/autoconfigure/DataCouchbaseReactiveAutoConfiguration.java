/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.autoconfigure;

import com.couchbase.client.java.Cluster;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;
import org.springframework.data.couchbase.repository.ReactiveCouchbaseRepository;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Reactive Couchbase
 * support.
 *
 * @author Alex Derkach
 * @since 4.0.0
 */
@AutoConfiguration(after = DataCouchbaseAutoConfiguration.class)
@ConditionalOnClass({ Cluster.class, ReactiveCouchbaseRepository.class, Flux.class })
@Import(DataCouchbaseReactiveConfiguration.class)
public final class DataCouchbaseReactiveAutoConfiguration {

}

/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.autoconfigure;

import java.util.List;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.jspecify.annotations.Nullable;

/**
 * A factory for a reactive {@link MongoClient}.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 4.0.0
 */
public class ReactiveMongoClientFactory extends MongoClientFactorySupport<MongoClient> {

	/**
	 * Construct a factory for creating a {@link MongoClient}.
	 * @param builderCustomizers a list of configuration settings customizers
	 */
	public ReactiveMongoClientFactory(@Nullable List<MongoClientSettingsBuilderCustomizer> builderCustomizers) {
		super(builderCustomizers, MongoClients::create);
	}

}

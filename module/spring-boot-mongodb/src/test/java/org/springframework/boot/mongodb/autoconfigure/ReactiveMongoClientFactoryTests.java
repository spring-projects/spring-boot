/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.autoconfigure;

import java.util.List;

import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.internal.MongoClientImpl;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@link ReactiveMongoClientFactory}.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class ReactiveMongoClientFactoryTests extends MongoClientFactorySupportTests<MongoClient> {

	@Override
	protected MongoClient createMongoClient(@Nullable List<MongoClientSettingsBuilderCustomizer> customizers,
			MongoClientSettings settings) {
		return new ReactiveMongoClientFactory(customizers).createMongoClient(settings);
	}

	@Override
	protected MongoClientSettings getClientSettings(MongoClient client) {
		return ((MongoClientImpl) client).getSettings();
	}

}

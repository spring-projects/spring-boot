/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.autoconfigure;

import java.util.List;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.internal.MongoClientImpl;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@link MongoClientFactory}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Mark Paluch
 * @author Scott Frederick
 */
class MongoClientFactoryTests extends MongoClientFactorySupportTests<MongoClient> {

	@Override
	protected MongoClient createMongoClient(@Nullable List<MongoClientSettingsBuilderCustomizer> customizers,
			MongoClientSettings settings) {
		return new MongoClientFactory(customizers).createMongoClient(settings);
	}

	@Override
	protected MongoClientSettings getClientSettings(MongoClient client) {
		return ((MongoClientImpl) client).getSettings();
	}

}

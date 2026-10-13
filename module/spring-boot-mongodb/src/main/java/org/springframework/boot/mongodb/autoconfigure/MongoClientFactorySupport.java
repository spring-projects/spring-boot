/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.autoconfigure;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.MongoDriverInformation;
import org.jspecify.annotations.Nullable;

/**
 * Base class for setup that is common to MongoDB client factories.
 *
 * @param <T> the mongo client type
 * @author Christoph Strobl
 * @author Scott Frederick
 * @since 4.0.0
 */
public abstract class MongoClientFactorySupport<T> {

	private final List<MongoClientSettingsBuilderCustomizer> builderCustomizers;

	private final BiFunction<MongoClientSettings, MongoDriverInformation, T> clientCreator;

	protected MongoClientFactorySupport(@Nullable List<MongoClientSettingsBuilderCustomizer> builderCustomizers,
			BiFunction<MongoClientSettings, MongoDriverInformation, T> clientCreator) {
		this.builderCustomizers = (builderCustomizers != null) ? builderCustomizers : Collections.emptyList();
		this.clientCreator = clientCreator;
	}

	public T createMongoClient(MongoClientSettings settings) {
		Builder targetSettings = MongoClientSettings.builder(settings);
		customize(targetSettings);
		return this.clientCreator.apply(targetSettings.build(), driverInformation());
	}

	private void customize(Builder builder) {
		for (MongoClientSettingsBuilderCustomizer customizer : this.builderCustomizers) {
			customizer.customize(builder);
		}
	}

	private MongoDriverInformation driverInformation() {
		return MongoDriverInformation.builder(MongoDriverInformation.builder().build())
			.driverName("spring-boot")
			.build();
	}

}

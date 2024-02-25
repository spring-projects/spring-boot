/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.mongo;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.MongoDriverInformation;

/**
 * Base class for setup that is common to MongoDB client factories.
 *
 * @param <T> the mongo client type
 * @author Christoph Strobl
 * @author Scott Frederick
 * @since 2.3.0
 */
public abstract class MongoClientFactorySupport<T> {

	private final List<MongoClientSettingsBuilderCustomizer> builderCustomizers;

	private final BiFunction<MongoClientSettings, MongoDriverInformation, T> clientCreator;

	/**
     * Constructs a new MongoClientFactorySupport with the specified list of builder customizers and client creator.
     * 
     * @param builderCustomizers the list of builder customizers to customize the MongoClientSettings builder (can be null)
     * @param clientCreator the function used to create the MongoClient instance from the MongoClientSettings and MongoDriverInformation
     */
    protected MongoClientFactorySupport(List<MongoClientSettingsBuilderCustomizer> builderCustomizers,
			BiFunction<MongoClientSettings, MongoDriverInformation, T> clientCreator) {
		this.builderCustomizers = (builderCustomizers != null) ? builderCustomizers : Collections.emptyList();
		this.clientCreator = clientCreator;
	}

	/**
     * Creates a new MongoDB client using the provided settings.
     * 
     * @param settings the settings for the MongoDB client
     * @return the created MongoDB client
     */
    public T createMongoClient(MongoClientSettings settings) {
		Builder targetSettings = MongoClientSettings.builder(settings);
		customize(targetSettings);
		return this.clientCreator.apply(targetSettings.build(), driverInformation());
	}

	/**
     * Customizes the given {@link Builder} by applying all registered {@link MongoClientSettingsBuilderCustomizer}s.
     * 
     * @param builder the {@link Builder} to be customized
     */
    private void customize(Builder builder) {
		for (MongoClientSettingsBuilderCustomizer customizer : this.builderCustomizers) {
			customizer.customize(builder);
		}
	}

	/**
     * Returns the MongoDriverInformation object containing information about the MongoDB driver used by the application.
     *
     * @return the MongoDriverInformation object
     */
    private MongoDriverInformation driverInformation() {
		return MongoDriverInformation.builder(MongoDriverInformation.builder().build())
			.driverName("spring-boot")
			.build();
	}

}

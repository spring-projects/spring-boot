/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.List;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;

import org.springframework.core.env.Environment;

/**
 * A factory for a reactive {@link MongoClient}.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 2.0.0
 */
public class ReactiveMongoClientFactory extends MongoClientFactorySupport<MongoClient> {

	/**
	 * Construct a factory for creating a {@link MongoClient}.
	 * @param properties configuration properties
	 * @param environment a Spring {@link Environment} containing configuration properties
	 * @param builderCustomizers a list of configuration settings customizers
	 * @deprecated since 2.4.0 in favor of {@link #ReactiveMongoClientFactory(List)}
	 */
	@Deprecated
	public ReactiveMongoClientFactory(MongoProperties properties, Environment environment,
			List<MongoClientSettingsBuilderCustomizer> builderCustomizers) {
		super(properties, environment, builderCustomizers, MongoClients::create);
	}

	/**
	 * Construct a factory for creating a {@link MongoClient}.
	 * @param builderCustomizers a list of configuration settings customizers
	 */
	public ReactiveMongoClientFactory(List<MongoClientSettingsBuilderCustomizer> builderCustomizers) {
		super(builderCustomizers, MongoClients::create);
	}

}

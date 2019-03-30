/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.mongo;

import org.bson.Document;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.util.Assert;

/**
 * A {@link ReactiveHealthIndicator} for Mongo.
 *
 * @author Yulin Qin
 * @since 2.0.0
 */
public class MongoReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private final ReactiveMongoTemplate reactiveMongoTemplate;

	public MongoReactiveHealthIndicator(ReactiveMongoTemplate reactiveMongoTemplate) {
		Assert.notNull(reactiveMongoTemplate, "ReactiveMongoTemplate must not be null");
		this.reactiveMongoTemplate = reactiveMongoTemplate;
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		Mono<Document> buildInfo = this.reactiveMongoTemplate
				.executeCommand("{ buildInfo: 1 }");
		return buildInfo.map((document) -> up(builder, document));
	}

	private Health up(Health.Builder builder, Document document) {
		return builder.up().withDetail("version", document.getString("version")).build();
	}

}

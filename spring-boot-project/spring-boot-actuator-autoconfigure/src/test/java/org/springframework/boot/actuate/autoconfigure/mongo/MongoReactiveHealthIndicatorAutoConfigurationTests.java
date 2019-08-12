/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.mongo;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.health.ApplicationHealthIndicator;
import org.springframework.boot.actuate.mongo.MongoHealthIndicator;
import org.springframework.boot.actuate.mongo.MongoReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoReactiveHealthIndicatorAutoConfiguration}.
 *
 * @author Yulin Qin
 */
class MongoReactiveHealthIndicatorAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
					MongoReactiveAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class,
					MongoReactiveHealthIndicatorAutoConfiguration.class, HealthIndicatorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MongoReactiveHealthIndicator.class)
				.doesNotHaveBean(MongoHealthIndicator.class).doesNotHaveBean(ApplicationHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.mongo.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(MongoReactiveHealthIndicator.class)
						.doesNotHaveBean(MongoHealthIndicator.class).hasSingleBean(ApplicationHealthIndicator.class));
	}

}

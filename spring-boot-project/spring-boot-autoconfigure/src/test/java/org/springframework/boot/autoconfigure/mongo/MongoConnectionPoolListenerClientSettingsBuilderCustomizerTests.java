/*
 * Copyright 2021-2021 the original author or authors.
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

import com.mongodb.MongoClientSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.event.ConnectionPoolListener;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoConnectionPoolListenerClientSettingsBuilderCustomizer}.
 *
 * @author Jonatan Ivanov
 */
public class MongoConnectionPoolListenerClientSettingsBuilderCustomizerTests {

	@Test
	void shouldNotSetUpAnyConnectionPoolListeners() {
		ConnectionPoolSettings connectionPoolSettings = buildAndCustomizeMongoClientSettings(Lists.list());
		assertThat(connectionPoolSettings.getConnectionPoolListeners()).isEmpty();
	}

	@Test
	void shouldSetUpConnectionPoolListeners() {
		List<ConnectionPoolListener> connectionPoolListeners = Lists.list(createConnectionPoolListener(),
				createConnectionPoolListener(), createConnectionPoolListener());
		ConnectionPoolSettings connectionPoolSettings = buildAndCustomizeMongoClientSettings(connectionPoolListeners);
		assertThat(connectionPoolSettings.getConnectionPoolListeners()).isEqualTo(connectionPoolListeners);
	}

	private ConnectionPoolSettings buildAndCustomizeMongoClientSettings(List<ConnectionPoolListener> listeners) {
		MongoClientSettings.Builder builder = MongoClientSettings.builder();
		new MongoConnectionPoolListenerClientSettingsBuilderCustomizer(listeners).customize(builder);

		return builder.build().getConnectionPoolSettings();
	}

	private ConnectionPoolListener createConnectionPoolListener() {
		return new ConnectionPoolListener() {
		};
	}

}

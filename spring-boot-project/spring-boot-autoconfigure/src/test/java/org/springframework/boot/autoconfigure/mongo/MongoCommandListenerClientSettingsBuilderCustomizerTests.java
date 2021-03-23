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
import com.mongodb.event.CommandListener;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoCommandListenerClientSettingsBuilderCustomizer}.
 *
 * @author Jonatan Ivanov
 */
class MongoCommandListenerClientSettingsBuilderCustomizerTests {

	@Test
	void shouldNotSetUpAnyCommandListeners() {
		MongoClientSettings mongoClientSettings = buildAndCustomizeMongoClientSettings(Lists.list());
		assertThat(mongoClientSettings.getCommandListeners()).isEmpty();
	}

	@Test
	void shouldSetUpCommandListeners() {
		List<CommandListener> commandListeners = Lists.list(createCommandListener(), createCommandListener(),
				createCommandListener());
		MongoClientSettings mongoClientSettings = buildAndCustomizeMongoClientSettings(commandListeners);
		assertThat(mongoClientSettings.getCommandListeners()).isEqualTo(commandListeners);
	}

	private MongoClientSettings buildAndCustomizeMongoClientSettings(List<CommandListener> listeners) {
		MongoClientSettings.Builder builder = MongoClientSettings.builder();
		new MongoCommandListenerClientSettingsBuilderCustomizer(listeners).customize(builder);

		return builder.build();
	}

	private CommandListener createCommandListener() {
		return new CommandListener() {
		};
	}

}

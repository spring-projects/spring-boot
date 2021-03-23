/*
 * Copyright 2021 the original author or authors.
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

import com.mongodb.MongoClientSettings;
import com.mongodb.event.CommandListener;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Adds {@link CommandListener} instances to {@link MongoClientSettings} through
 * {@link MongoClientSettingsBuilderCustomizer}.
 *
 * @author Jonatan Ivanov
 * @since 2.5.0
 */
public class MongoCommandListenerClientSettingsBuilderCustomizer implements MongoClientSettingsBuilderCustomizer {

	private final Iterable<CommandListener> commandListeners;

	public MongoCommandListenerClientSettingsBuilderCustomizer(@NonNull Iterable<CommandListener> commandListeners) {
		this.commandListeners = commandListeners;
	}

	@Override
	public void customize(MongoClientSettings.Builder clientSettingsBuilder) {
		for (CommandListener commandListener : this.commandListeners) {
			clientSettingsBuilder.addCommandListener(commandListener);
		}
	}

}

/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.mongo.embedded;

import de.flapdoodle.embed.process.config.store.DownloadConfig;
import de.flapdoodle.embed.process.config.store.ImmutableDownloadConfig;
import de.flapdoodle.embed.process.config.store.ImmutableDownloadConfig.Builder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link DownloadConfig} via a {@link Builder} whilst retaining default
 * auto-configuration.
 *
 * @author Michael Gmeiner
 * @since 2.2.0
 */
@FunctionalInterface
public interface DownloadConfigBuilderCustomizer {

	/**
	 * Customize the {@link Builder}.
	 * @param downloadConfigBuilder the {@link Builder} to customize
	 */
	void customize(ImmutableDownloadConfig.Builder downloadConfigBuilder);

}

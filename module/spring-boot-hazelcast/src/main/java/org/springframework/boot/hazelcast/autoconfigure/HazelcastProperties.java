/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.hazelcast.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Configuration properties for the hazelcast integration.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@ConfigurationProperties("spring.hazelcast")
public class HazelcastProperties {

	/**
	 * The location of the configuration file to use to initialize Hazelcast.
	 */
	private @Nullable Resource config;

	public @Nullable Resource getConfig() {
		return this.config;
	}

	public void setConfig(@Nullable Resource config) {
		this.config = config;
	}

	/**
	 * Resolve the config location if set.
	 * @return the location or {@code null} if it is not set
	 * @throws IllegalArgumentException if the config attribute is set to an unknown
	 * location
	 */
	public @Nullable Resource resolveConfigLocation() {
		Resource config = this.config;
		if (config == null) {
			return null;
		}
		Assert.state(config.exists(), () -> "Hazelcast configuration does not exist '" + config.getDescription() + "'");
		return config;
	}

}

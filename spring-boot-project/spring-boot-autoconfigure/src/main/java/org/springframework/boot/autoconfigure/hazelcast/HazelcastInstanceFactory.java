/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.IOException;
import java.net.URL;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Factory that can be used to create a {@link HazelcastInstance}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.3.0
 */
public class HazelcastInstanceFactory {

	private final Config config;

	/**
	 * Create a {@link HazelcastInstanceFactory} for the specified configuration location.
	 * @param configLocation the location of the configuration file
	 * @throws IOException if the configuration location could not be read
	 */
	public HazelcastInstanceFactory(@NonNull Resource configLocation) throws IOException {
		this.config = getConfig(configLocation);
	}

	/**
	 * Create a {@link HazelcastInstanceFactory} for the specified configuration.
	 * @param config the configuration
	 */
	public HazelcastInstanceFactory(@NonNull Config config) {
		this.config = config;
	}

	private Config getConfig(Resource configLocation) throws IOException {
		URL configUrl = configLocation.getURL();
		Config config = new XmlConfigBuilder(configUrl).build();
		if (ResourceUtils.isFileURL(configUrl)) {
			config.setConfigurationFile(configLocation.getFile());
		}
		else {
			config.setConfigurationUrl(configUrl);
		}
		return config;
	}

	/**
	 * Get the {@link HazelcastInstance}.
	 * @return the {@link HazelcastInstance}
	 */
	public HazelcastInstance getHazelcastInstance() {
		if (StringUtils.hasText(this.config.getInstanceName())) {
			return Hazelcast.getOrCreateHazelcastInstance(this.config);
		}
		return Hazelcast.newHazelcastInstance(this.config);
	}

}

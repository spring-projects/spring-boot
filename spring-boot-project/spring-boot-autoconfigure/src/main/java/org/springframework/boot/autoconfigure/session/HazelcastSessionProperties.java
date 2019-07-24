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

package org.springframework.boot.autoconfigure.session;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.session.hazelcast.HazelcastFlushMode;

/**
 * Configuration properties for Hazelcast backed Spring Session.
 *
 * @author Vedran Pavic
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.session.hazelcast")
public class HazelcastSessionProperties {

	/**
	 * Name of the map used to store sessions.
	 */
	private String mapName = "spring:session:sessions";

	/**
	 * Sessions flush mode.
	 */
	private HazelcastFlushMode flushMode = HazelcastFlushMode.ON_SAVE;

	public String getMapName() {
		return this.mapName;
	}

	public void setMapName(String mapName) {
		this.mapName = mapName;
	}

	public HazelcastFlushMode getFlushMode() {
		return this.flushMode;
	}

	public void setFlushMode(HazelcastFlushMode flushMode) {
		this.flushMode = flushMode;
	}

}

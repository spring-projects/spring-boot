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
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;

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
	 * Sessions flush mode. Determines when session changes are written to the session
	 * store.
	 */
	private FlushMode flushMode = FlushMode.ON_SAVE;

	/**
	 * Sessions save mode. Determines how session changes are tracked and saved to the
	 * session store.
	 */
	private SaveMode saveMode = SaveMode.ON_SET_ATTRIBUTE;

	/**
     * Returns the name of the map.
     *
     * @return the name of the map
     */
    public String getMapName() {
		return this.mapName;
	}

	/**
     * Sets the name of the map.
     * 
     * @param mapName the name of the map
     */
    public void setMapName(String mapName) {
		this.mapName = mapName;
	}

	/**
     * Returns the flush mode of the session.
     *
     * @return the flush mode of the session
     */
    public FlushMode getFlushMode() {
		return this.flushMode;
	}

	/**
     * Sets the flush mode for the session.
     * 
     * @param flushMode the flush mode to be set
     */
    public void setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
	}

	/**
     * Returns the save mode of the Hazelcast session properties.
     *
     * @return the save mode of the Hazelcast session properties
     */
    public SaveMode getSaveMode() {
		return this.saveMode;
	}

	/**
     * Sets the save mode for the Hazelcast session properties.
     * 
     * @param saveMode the save mode to be set
     */
    public void setSaveMode(SaveMode saveMode) {
		this.saveMode = saveMode;
	}

}

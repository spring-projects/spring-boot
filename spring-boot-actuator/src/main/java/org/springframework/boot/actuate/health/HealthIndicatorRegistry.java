/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.Map;
import java.util.Set;

/**
 * A registry of {@link HealthIndicator} instances.
 *
 * @author Vedran Pavic
 */
public interface HealthIndicatorRegistry {

	/**
	 * Register the health indicator with given name.
	 * @param name the health indicator name
	 * @param healthIndicator the health indicator to register
	 */
	void register(String name, HealthIndicator healthIndicator);

	/**
	 * Unregister the health indicator with given name.
	 * @param name the health indicator name
	 */
	void unregister(String name);

	/**
	 * Return names of all currently registered health indicators.
	 * @return the health indicator names
	 */
	Set<String> getRegisteredNames();

	/**
	 * Return an indication of health for given health indicator name.
	 * @param name the health indicator name
	 * @return the health
	 */
	Health runHealthIndicator(String name);

	/**
	 * Return a map of healths for all registered health indicators.
	 * @return the map of healths
	 */
	Map<String, Health> runHealthIndicators();

}

/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.influx;

import org.influxdb.InfluxDB;

/**
 * Callback interface that can be implemented by beans wishing to further customize
 * {@code InfluxDB} whilst retaining default auto-configuration.
 *
 * @author Eddú Meléndez
 * @since 2.5.0
 * @deprecated since 3.2.0 for removal in 3.4.0 in favor of the
 * <a href="https://github.com/influxdata/influxdb-client-java">new client</a> and its own
 * Spring Boot integration.
 */
@FunctionalInterface
@Deprecated(since = "3.2.0", forRemoval = true)
public interface InfluxDbCustomizer {

	/**
	 * Customize the {@link InfluxDB}.
	 * @param influxDb the InfluxDB instance to customize
	 */
	void customize(InfluxDB influxDb);

}

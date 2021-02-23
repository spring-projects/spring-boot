/*
 * Copyright 2012-2021 the original author or authors.
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

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientOptions;

/**
 * Callback interface that can be implemented by beans wishing to further customize
 * {@link InfluxDBClientOptions} used to configure an {@link InfluxDBClient} whilst
 * retaining default auto-configuration.
 *
 * @author Stephane Nicoll
 * @since 2.6.0
 */
@FunctionalInterface
public interface InfluxDbClientOptionsBuilderCustomizer {

	/**
	 * Customize the {@link InfluxDBClientOptions}.
	 * @param builder the influxDB client options builder to customize
	 */
	void customize(InfluxDBClientOptions.Builder builder);

}

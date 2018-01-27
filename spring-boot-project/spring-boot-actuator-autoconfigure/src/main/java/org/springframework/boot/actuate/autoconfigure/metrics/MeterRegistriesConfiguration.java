/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import org.springframework.boot.actuate.autoconfigure.metrics.export.atlas.AtlasExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.datadog.DatadogExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ganglia.GangliaExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.graphite.GraphiteExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.influx.InfluxExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.jmx.JmxExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.statsd.StatsdExportConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Imports for registry configurations.
 *
 * @author Jon Schneider
 */
@Configuration
@Import({ AtlasExportConfiguration.class, DatadogExportConfiguration.class,
		GangliaExportConfiguration.class, GraphiteExportConfiguration.class,
		InfluxExportConfiguration.class, JmxExportConfiguration.class,
		PrometheusExportConfiguration.class, SimpleExportConfiguration.class,
		StatsdExportConfiguration.class })
class MeterRegistriesConfiguration {

}

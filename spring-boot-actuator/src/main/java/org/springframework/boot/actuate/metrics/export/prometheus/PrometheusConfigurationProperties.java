/**
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
package org.springframework.boot.actuate.metrics.export.prometheus;


import org.springframework.boot.actuate.metrics.export.RegistryConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.micrometer.prometheus.PrometheusConfig;

/**
 * Exists solely to aid in autocompletion of Prometheus enablement in .properties and .yml.
 *
 * @since 2.0.0
 * @author Jon Schneider
 */
@ConfigurationProperties(prefix = "metrics.prometheus")
public class PrometheusConfigurationProperties extends RegistryConfigurationProperties implements PrometheusConfig {
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setDescriptions(Boolean descriptions) {
        set("descriptions", descriptions);
    }

    @Override
    public String prefix() {
        return "metrics.prometheus";
    }
}

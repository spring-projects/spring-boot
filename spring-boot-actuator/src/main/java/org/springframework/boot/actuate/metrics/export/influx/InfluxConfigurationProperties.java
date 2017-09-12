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
package org.springframework.boot.actuate.metrics.export.influx;

import org.springframework.boot.actuate.metrics.export.StepRegistryConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxConsistency;

/**
 * @since 2.0.0
 * @author Jon Schneider
 */
@ConfigurationProperties(prefix = "metrics.influx")
public class InfluxConfigurationProperties extends StepRegistryConfigurationProperties implements InfluxConfig {
    public void setDb(String db) {
        set("db", db);
    }

    public void setConsistency(InfluxConsistency consistency) {
        set("consistency", consistency);
    }

    public void setUserName(String userName) {
        set("userName", userName);
    }

    public void setPassword(String password) {
        set("password", password);
    }

    public void setRetentionPolicy(String retentionPolicy) {
        set("retentionPolicy", retentionPolicy);
    }

    public void setUri(String uri) {
        set("uri", uri);
    }

    public void setCompressed(Boolean compressed) {
        set("compressed", compressed);
    }

    @Override
    public String prefix() {
        return "metrics.influx";
    }
}

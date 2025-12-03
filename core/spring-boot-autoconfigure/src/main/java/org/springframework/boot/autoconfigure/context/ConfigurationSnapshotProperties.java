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

package org.springframework.boot.autoconfigure.context;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for configuration snapshot logging.
 *
 * @author Your Name
 * @since 3.2.0
 */
@ConfigurationProperties(prefix = "spring.config-snapshot")
public class ConfigurationSnapshotProperties {

    /**
     * Whether to enable configuration snapshot logging.
     */
    private boolean enabled = false;

    /**
     * Where to log the configuration snapshot.
     */
    private LogTo logTo = LogTo.CONSOLE;

    /**
     * List of properties to include in the snapshot.
     */
    private List<String> include;

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LogTo getLogTo() {
        return this.logTo;
    }

    public void setLogTo(LogTo logTo) {
        this.logTo = logTo;
    }

    public List<String> getInclude() {
        return this.include;
    }

    public void setInclude(List<String> include) {
        this.include = include;
    }

    public enum LogTo {
        CONSOLE, FILE
    }
}

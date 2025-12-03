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

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * Application listener that logs a snapshot of the application configuration when the
 * context is refreshed.
 *
 * @author Your Name
 * @since 3.2.0
 */
public class ConfigurationSnapshotListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationSnapshotListener.class);

    private final ConfigurationSnapshotProperties properties;

    public ConfigurationSnapshotListener(ConfigurationSnapshotProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Environment environment = event.getApplicationContext().getEnvironment();
        if (!(environment instanceof ConfigurableEnvironment configurableEnvironment)) {
            return;
        }

        // Build configuration snapshot
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("activeProfiles", environment.getActiveProfiles());

        Map<String, String> includedProperties = new HashMap<>();
        if (this.properties.getInclude() != null && !this.properties.getInclude().isEmpty()) {
            for (String propertyName : this.properties.getInclude()) {
                if (environment.containsProperty(propertyName)) {
                    includedProperties.put(propertyName, environment.getProperty(propertyName));
                }
            }
        }
        snapshot.put("properties", includedProperties);

        // Convert to JSON
        String json = toJson(snapshot);

        // Log to console or file
        if (this.properties.getLogTo() == ConfigurationSnapshotProperties.LogTo.CONSOLE) {
            logger.info("Configuration snapshot: {}", json);
        } else {
            writeToFile(json);
        }
    }

    private String toJson(Map<String, Object> snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // Active profiles
        sb.append("\"activeProfiles\": [");
        String[] activeProfiles = (String[]) snapshot.get("activeProfiles");
        for (int i = 0; i < activeProfiles.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(activeProfiles[i]).append("\"");
        }
        sb.append("]");

        // Properties
        sb.append(",\"properties\": {");
        Map<String, String> properties = (Map<String, String>) snapshot.get("properties");
        int i = 0;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            i++;
        }
        sb.append("}");

        sb.append("}");
        return sb.toString();
    }

    private void writeToFile(String json) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = "config-snapshot-" + timestamp + ".log";

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(json);
            logger.info("Configuration snapshot written to file: {}", filename);
        } catch (IOException e) {
            logger.error("Failed to write configuration snapshot to file: {}", filename, e);
        }
    }
}

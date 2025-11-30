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

package org.springframework.boot.actuate.info;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * {@link Endpoint @Endpoint} to expose application overview information.
 *
 * @author Huang Hu
 * @since 4.0.1
 */
@Endpoint(id = "appoverview")
public class AppOverviewEndpoint {

    private final Environment environment;
    private final List<InfoContributor> infoContributors;
    private final Optional<BuildProperties> buildProperties;
    private final Instant startTime;
    private final AppOverviewEndpointProperties properties;

    /**
     * Create a new {@link AppOverviewEndpoint} instance.
     * @param environment the environment
     * @param infoContributors the info contributors to use
     * @param buildProperties the build properties
     * @param properties the configuration properties
     */
    public AppOverviewEndpoint(Environment environment, List<InfoContributor> infoContributors, Optional<BuildProperties> buildProperties, AppOverviewEndpointProperties properties) {
        Assert.notNull(environment, "'environment' must not be null");
        Assert.notNull(infoContributors, "'infoContributors' must not be null");
        Assert.notNull(buildProperties, "'buildProperties' must not be null");
        Assert.notNull(properties, "'properties' must not be null");
        this.environment = environment;
        this.infoContributors = infoContributors;
        this.buildProperties = buildProperties;
        this.startTime = Instant.now();
        this.properties = properties;
    }

    @ReadOperation
    public Map<String, Object> appOverview() {
        AppOverview.Builder builder = new AppOverview.Builder();
        
        // Add application name
        builder.withName(environment.getProperty("spring.application.name", "unknown"));
        
        // Add application version if configured
        if (properties.isShowVersion()) {
            buildProperties.ifPresent(build -> builder.withVersion(build.getVersion()));
        }
        
        // Add start time and uptime
        builder.withStartTime(startTime);
        builder.withUptime(Duration.between(startTime, Instant.now()));
        
        // Add active profiles if configured
        if (properties.isShowProfiles()) {
            String[] activeProfiles = environment.getActiveProfiles();
            if (activeProfiles.length > 0) {
                builder.withActiveProfiles(List.of(activeProfiles));
            }
        }
        
        // Add info from info contributors
        Info.Builder infoBuilder = new Info.Builder();
        for (InfoContributor contributor : infoContributors) {
            contributor.contribute(infoBuilder);
        }
        builder.withInfo(infoBuilder.build().getDetails());
        
        return OperationResponseBody.of(builder.build().getDetails());
    }

    /**
     * Application overview information.
     */
    public static class AppOverview {

        private final Map<String, Object> details;

        private AppOverview(Map<String, Object> details) {
            this.details = details;
        }

        public Map<String, Object> getDetails() {
            return this.details;
        }

        /**
         * Builder for {@link AppOverview}.
         */
        public static class Builder {

            private final Info.Builder infoBuilder = new Info.Builder();

            public Builder withName(String name) {
                this.infoBuilder.withDetail("name", name);
                return this;
            }

            public Builder withVersion(String version) {
                this.infoBuilder.withDetail("version", version);
                return this;
            }

            public Builder withStartTime(Instant startTime) {
                this.infoBuilder.withDetail("startTime", startTime);
                return this;
            }

            public Builder withUptime(Duration uptime) {
                this.infoBuilder.withDetail("uptime", uptime.toMillis());
                return this;
            }

            public Builder withActiveProfiles(List<String> activeProfiles) {
                this.infoBuilder.withDetail("activeProfiles", activeProfiles);
                return this;
            }

            public Builder withInfo(Map<String, Object> info) {
                info.forEach(this.infoBuilder::withDetail);
                return this;
            }

            public AppOverview build() {
                return new AppOverview(this.infoBuilder.build().getDetails());
            }

        }

    }

    /**
     * Configuration properties for {@link AppOverviewEndpoint}.
     */
    @ConfigurationProperties(prefix = "management.endpoint.app-overview")
    public static class AppOverviewEndpointProperties {

        private boolean enabled = true;
        private boolean showProfiles = true;
        private boolean showVersion = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isShowProfiles() {
            return showProfiles;
        }

        public void setShowProfiles(boolean showProfiles) {
            this.showProfiles = showProfiles;
        }

        public boolean isShowVersion() {
            return showVersion;
        }

        public void setShowVersion(boolean showVersion) {
            this.showVersion = showVersion;
        }
    }

}
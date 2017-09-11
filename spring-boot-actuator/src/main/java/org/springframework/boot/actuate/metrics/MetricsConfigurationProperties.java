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
package org.springframework.boot.actuate.metrics;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * @since 2.0.0
 * @author Jon Schneider
 */
@ConfigurationProperties("metrics")
public class MetricsConfigurationProperties {
    private Web web = new Web();

    /**
     * Determines whether {@link MeterRegistry} implementations configured by Spring should be
     * bound to the global static registry on {@link io.micrometer.core.instrument.Metrics}.
     * For Spring Boot tests involving metrics, set this to {@code false} to maximize test independence.
     * Otherwise, it can be left to {@code true}.
     */
    private Boolean useGlobalRegistry = true;

    public Boolean getUseGlobalRegistry() {
        return useGlobalRegistry;
    }

    public void setUseGlobalRegistry(Boolean useGlobalRegistry) {
        this.useGlobalRegistry = useGlobalRegistry;
    }

    public static class Web {
        /**
         * Determines whether every request mapping (WebMVC or Webflux) should be automatically timed.
         * If the number of time series emitted from a Spring application grows too large on account
         * of request mapping timings, disable this and use {@link io.micrometer.core.annotation.Timed}
         * on a per request mapping basis as needed.
         */
        private Boolean autoTimeServerRequests = true;

        private String serverRequestsName = "http.server.requests";

        /**
         * Determines whether instrumented server requests ship percentiles histogram buckets by default.
         * The default can be overriden by adding {@link io.micrometer.core.annotation.Timed} to a request
         * endpoint and setting {@code percentiles} to true.
         */
        private Boolean serverRequestPercentiles = false;

        private String clientRequestsName = "http.client.requests";

        /**
         * Determines whether instrumented client requests ship percentiles histogram buckets by default.
         */
        private Boolean clientRequestPercentiles = false;

        public Boolean getAutoTimeServerRequests() {
            return autoTimeServerRequests;
        }

        public void setAutoTimeServerRequests(Boolean autoTimeServerRequests) {
            this.autoTimeServerRequests = autoTimeServerRequests;
        }

        public void setServerRequestsName(String serverRequestsName) {
            this.serverRequestsName = serverRequestsName;
        }

        public String getServerRequestsName() {
            return serverRequestsName;
        }

        public void setClientRequestsName(String clientRequestsName) {
            this.clientRequestsName = clientRequestsName;
        }

        public String getClientRequestsName() {
            return clientRequestsName;
        }

        public void setServerRequestPercentiles(Boolean serverRequestPercentiles) {
            this.serverRequestPercentiles = serverRequestPercentiles;
        }

        public Boolean getServerRequestPercentiles() {
            return serverRequestPercentiles;
        }

        public void setClientRequestPercentiles(Boolean clientRequestPercentiles) {
            this.clientRequestPercentiles = clientRequestPercentiles;
        }

        public Boolean getClientRequestPercentiles() {
            return clientRequestPercentiles;
        }
    }

    public Web getWeb() {
        return web;
    }
}

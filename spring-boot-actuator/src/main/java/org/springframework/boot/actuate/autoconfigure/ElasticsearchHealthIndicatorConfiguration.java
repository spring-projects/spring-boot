/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import org.elasticsearch.client.Client;
import org.springframework.boot.actuate.health.ElasticsearchHealthIndicator;
import org.springframework.boot.actuate.health.ElasticsearchHealthIndicatorProperties;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for
 * {@link org.springframework.boot.actuate.health.ElasticsearchHealthIndicator}.
 *
 * @author Binwei Yang
 * @since 1.2.2
 */
@Configuration
@AutoConfigureBefore({EndpointAutoConfiguration.class})
@AutoConfigureAfter({HealthIndicatorAutoConfiguration.class})
@ConditionalOnProperty(prefix = "management.health.elasticsearch", name = "enabled", matchIfMissing = true)
public class ElasticsearchHealthIndicatorConfiguration {

    @Bean
    @ConditionalOnBean(Client.class)
    @ConditionalOnMissingBean(name = "elasticsearchHealthIndicator")
    public HealthIndicator elasticsearchHealthIndicator() {
        return new ElasticsearchHealthIndicator();
    }

    @Bean
    public ElasticsearchHealthIndicatorProperties elasticsearchHealthIndicatorProperties() {
        return new ElasticsearchHealthIndicatorProperties();
    }
}

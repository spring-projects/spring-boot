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

package org.springframework.boot.actuate.health;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simple implementation of a {@link HealthIndicator} returning health status information for
 * ElasticSearch cluster.
 *
 * @author Binwei Yang
 * @since 1.2.2
 */
public class ElasticsearchHealthIndicator extends ApplicationHealthIndicator {
    @Autowired
    private Client client;

    @Autowired
    private ElasticsearchHealthIndicatorProperties properties;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try {
            ClusterHealthResponse response = client.admin().cluster().health(Requests.clusterHealthRequest(
                    properties.getIndexNamesAsArray()
            )).actionGet(100);

            switch (response.getStatus()) {
                case GREEN:
                    builder.up();
                    break;
                case RED:
                    builder.down();
                    break;
                case YELLOW:
                default:
                    builder.unknown();
                    break;
            }
            builder.withDetail("clusterHealth", response);
        } catch (Exception handled) {
            builder.unknown().withDetail("exception", handled);
        }
    }
}

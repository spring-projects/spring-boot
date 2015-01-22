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

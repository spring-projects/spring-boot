package org.springframework.boot.actuate.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * External configuration properties for {@link ElasticsearchHealthIndicator}
 *
 * @author Binwei Yang
 * @since 1.2.2
 */
@ConfigurationProperties("management.health.elasticsearch")
public class ElasticsearchHealthIndicatorProperties {

    public static final String ALL = "_all";

    /**
     * comma separated index names. the default includes all indices.
     */
    private String indices = ALL;

    public String getIndices() {
        return indices;
    }

    public void setIndices(String indices) {
        this.indices = indices;
    }

    String[] getIndexNamesAsArray() {
        if (null == indices) {
            return new String[]{ALL};
        } else {
            return indices.split(",");
        }
    }
}

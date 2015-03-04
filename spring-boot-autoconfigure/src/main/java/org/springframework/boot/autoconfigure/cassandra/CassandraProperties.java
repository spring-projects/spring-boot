/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.ReconnectionPolicy;
import com.datastax.driver.core.policies.RetryPolicy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Cassandra.
 *
 * @author Julien Dubois
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.data.cassandra")
public class CassandraProperties {

    private static final Log logger = LogFactory.getLog(CassandraProperties.class);

    /**
     * Name of the Cassandra cluster.
     */
    private String clusterName = "Test Cluster";

    private int port = ProtocolOptions.DEFAULT_PORT;

    /**
     * Comma-separated list of cluster node addresses.
     */
    private String contactPoints = "localhost";

    /**
     * Compression supported by the Cassandra binary protocol: can be NONE, SNAPPY, LZ4.
     */
    private String compression = ProtocolOptions.Compression.NONE.name();

    /**
     * Class name of the load balancing policy.
     */
    private String loadBalancingPolicy;

    /**
     * Queries consistency level.
     */
    private String consistency;

    /**
     * Queries serial consistency level.
     */
    private String serialConsistency;

    /**
     * Queries default fetch size.
     */
    private int fetchSize = QueryOptions.DEFAULT_FETCH_SIZE;

    /**
     * Class name of the reconnection policy.
     */
    private String reconnectionPolicy;

    /**
     * Class name of the retry policy.
     */
    private String retryPolicy;

    /**
     * Socket option: connection time out.
     */
    private int connectTimeoutMillis = SocketOptions.DEFAULT_CONNECT_TIMEOUT_MILLIS;

    /**
     * Socket option: read time out.
     */
    private int readTimeoutMillis = SocketOptions.DEFAULT_READ_TIMEOUT_MILLIS;

    /**
     * Enable SSL support.
     */
    private boolean ssl = false;

    /**
     * Keyspace name to use.
     */
    private String keyspaceName;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getContactPoints() {
        return contactPoints;
    }

    public void setContactPoints(String contactPoints) {
        this.contactPoints = contactPoints;
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public String getLoadBalancingPolicy() {
        return loadBalancingPolicy;
    }

    public void setLoadBalancingPolicy(String loadBalancingPolicy) {
        this.loadBalancingPolicy = loadBalancingPolicy;
    }

    public String getConsistency() {
        return consistency;
    }

    public void setConsistency(String consistency) {
        this.consistency = consistency;
    }

    public String getSerialConsistency() {
        return serialConsistency;
    }

    public void setSerialConsistency(String serialConsistency) {
        this.serialConsistency = serialConsistency;
    }

    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public String getReconnectionPolicy() {
        return reconnectionPolicy;
    }

    public void setReconnectionPolicy(String reconnectionPolicy) {
        this.reconnectionPolicy = reconnectionPolicy;
    }

    public String getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(String retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getKeyspaceName() {
        return keyspaceName;
    }

    public void setKeyspaceName(String keyspaceName) {
        this.keyspaceName = keyspaceName;
    }

    public Cluster createCluster() {
        Cluster.Builder builder = Cluster.builder()
                .withClusterName(this.getClusterName())
                .withPort(this.getPort());

        // Manage compression protocol
        if (ProtocolOptions.Compression.SNAPPY.equals(this.getCompression())) {
            builder.withCompression(ProtocolOptions.Compression.SNAPPY);
        } else if (ProtocolOptions.Compression.LZ4.equals(this.getCompression())) {
            builder.withCompression(ProtocolOptions.Compression.LZ4);
        } else {
            builder.withCompression(ProtocolOptions.Compression.NONE);
        }

        // Manage the load balancing policy
        if (!StringUtils.isEmpty(this.getLoadBalancingPolicy())) {
            try {
                Class loadBalancingPolicyClass = ClassUtils.forName(this.getLoadBalancingPolicy(), null);
                Object loadBalancingPolicyInstance = loadBalancingPolicyClass.newInstance();
                LoadBalancingPolicy userLoadBalancingPolicy = (LoadBalancingPolicy) loadBalancingPolicyInstance;
                builder.withLoadBalancingPolicy(userLoadBalancingPolicy);
            } catch (ClassNotFoundException e) {
                logger.warn("The load balancing policy could not be loaded, falling back to the default policy", e);
            } catch (InstantiationException e) {
                logger.warn("The load balancing policy could not be instanced, falling back to the default policy", e);
            } catch (IllegalAccessException e) {
                logger.warn("The load balancing policy could not be created, falling back to the default policy", e);
            } catch (ClassCastException e) {
                logger.warn("The load balancing policy does not implement the correct interface, falling back to the default policy", e);
            }
        }

        // Manage query options
        QueryOptions queryOptions = new QueryOptions();
        if (this.getConsistency() != null) {
            ConsistencyLevel consistencyLevel = ConsistencyLevel.valueOf(this.getConsistency());
            queryOptions.setConsistencyLevel(consistencyLevel);
        }
        if (this.getSerialConsistency() != null) {
            ConsistencyLevel serialConsistencyLevel = ConsistencyLevel.valueOf(this.getSerialConsistency());
            queryOptions.setSerialConsistencyLevel(serialConsistencyLevel);
        }
        queryOptions.setFetchSize(this.getFetchSize());
        builder.withQueryOptions(queryOptions);

        // Manage the reconnection policy
        if (!StringUtils.isEmpty(this.getReconnectionPolicy())) {
            try {
                Class reconnectionPolicyClass = ClassUtils.forName(this.getReconnectionPolicy(), null);
                Object reconnectionPolicyInstance = reconnectionPolicyClass.newInstance();
                ReconnectionPolicy userReconnectionPolicy = (ReconnectionPolicy) reconnectionPolicyInstance;
                builder.withReconnectionPolicy(userReconnectionPolicy);
            } catch (ClassNotFoundException e) {
                logger.warn("The reconnection policy could not be loaded, falling back to the default policy", e);
            } catch (InstantiationException e) {
                logger.warn("The reconnection policy could not be instanced, falling back to the default policy", e);
            } catch (IllegalAccessException e) {
                logger.warn("The reconnection policy could not be created, falling back to the default policy", e);
            } catch (ClassCastException e) {
                logger.warn("The reconnection policy does not implement the correct interface, falling back to the default policy", e);
            }
        }

        // Manage the retry policy
        if (!StringUtils.isEmpty(this.getRetryPolicy())) {
            try {
                Class retryPolicyClass = ClassUtils.forName(this.getRetryPolicy(), null);
                Object retryPolicyInstance = retryPolicyClass.newInstance();
                RetryPolicy userRetryPolicy = (RetryPolicy) retryPolicyInstance;
                builder.withRetryPolicy(userRetryPolicy);
            } catch (ClassNotFoundException e) {
                logger.warn("The retry policy could not be loaded, falling back to the default policy", e);
            } catch (InstantiationException e) {
                logger.warn("The retry policy could not be instanced, falling back to the default policy", e);
            } catch (IllegalAccessException e) {
                logger.warn("The retry policy could not be created, falling back to the default policy", e);
            } catch (ClassCastException e) {
                logger.warn("The retry policy does not implement the correct interface, falling back to the default policy", e);
            }
        }

        // Manage socket options
        SocketOptions socketOptions = new SocketOptions();
        socketOptions.setConnectTimeoutMillis(this.getConnectTimeoutMillis());
        socketOptions.setReadTimeoutMillis(this.getReadTimeoutMillis());
        builder.withSocketOptions(socketOptions);

        // Manage SSL
        if (this.isSsl()) {
            builder.withSSL();
        }

        // Manage the contact points
        builder.addContactPoints(StringUtils.commaDelimitedListToStringArray(this.getContactPoints()));

        return builder.build();
    }
}

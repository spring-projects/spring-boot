/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.couchbase;

import javax.net.ssl.TrustManagerFactory;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.codec.JacksonJsonSerializer;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.env.ClusterEnvironment.Builder;
import com.couchbase.client.java.json.JsonValueModule;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration.CouchbaseCondition;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties.Ssl;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties.Timeouts;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Couchbase.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Yulin Qin
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 1.4.0
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnClass(Cluster.class)
@Conditional(CouchbaseCondition.class)
@EnableConfigurationProperties(CouchbaseProperties.class)
public class CouchbaseAutoConfiguration {

	private final CouchbaseProperties properties;

	/**
     * Constructs a new instance of CouchbaseAutoConfiguration with the specified CouchbaseProperties.
     *
     * @param properties the CouchbaseProperties to be used for configuration
     */
    CouchbaseAutoConfiguration(CouchbaseProperties properties) {
		this.properties = properties;
	}

	/**
     * Creates a new instance of {@link PropertiesCouchbaseConnectionDetails} if no bean of type {@link CouchbaseConnectionDetails} is present.
     * 
     * This method retrieves the connection details from the properties and initializes a new instance of {@link PropertiesCouchbaseConnectionDetails} with the retrieved values.
     * 
     * @return a new instance of {@link PropertiesCouchbaseConnectionDetails} initialized with the connection details from the properties
     */
    @Bean
	@ConditionalOnMissingBean(CouchbaseConnectionDetails.class)
	PropertiesCouchbaseConnectionDetails couchbaseConnectionDetails() {
		return new PropertiesCouchbaseConnectionDetails(this.properties);
	}

	/**
     * Creates and configures a ClusterEnvironment bean for connecting to Couchbase.
     * 
     * @param connectionDetails the Couchbase connection details
     * @param customizers the customizers for modifying the ClusterEnvironment builder
     * @param sslBundles the SSL bundles for configuring secure connections
     * @return the configured ClusterEnvironment bean
     */
    @Bean
	@ConditionalOnMissingBean
	public ClusterEnvironment couchbaseClusterEnvironment(CouchbaseConnectionDetails connectionDetails,
			ObjectProvider<ClusterEnvironmentBuilderCustomizer> customizers, ObjectProvider<SslBundles> sslBundles) {
		Builder builder = initializeEnvironmentBuilder(connectionDetails, sslBundles.getIfAvailable());
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	/**
     * Creates a Couchbase Cluster bean.
     * 
     * @param couchbaseClusterEnvironment The ClusterEnvironment bean for configuring the Couchbase cluster.
     * @param connectionDetails The CouchbaseConnectionDetails bean containing the connection details for the cluster.
     * @return The created Cluster bean.
     */
    @Bean(destroyMethod = "disconnect")
	@ConditionalOnMissingBean
	public Cluster couchbaseCluster(ClusterEnvironment couchbaseClusterEnvironment,
			CouchbaseConnectionDetails connectionDetails) {
		ClusterOptions options = ClusterOptions
			.clusterOptions(connectionDetails.getUsername(), connectionDetails.getPassword())
			.environment(couchbaseClusterEnvironment);
		return Cluster.connect(connectionDetails.getConnectionString(), options);
	}

	/**
     * Initializes the ClusterEnvironment.Builder with the provided CouchbaseConnectionDetails and SslBundles.
     * 
     * @param connectionDetails The CouchbaseConnectionDetails containing the connection details for the cluster.
     * @param sslBundles The SslBundles containing the SSL configuration for the cluster.
     * @return The initialized ClusterEnvironment.Builder.
     */
    private ClusterEnvironment.Builder initializeEnvironmentBuilder(CouchbaseConnectionDetails connectionDetails,
			SslBundles sslBundles) {
		ClusterEnvironment.Builder builder = ClusterEnvironment.builder();
		Timeouts timeouts = this.properties.getEnv().getTimeouts();
		builder.timeoutConfig((config) -> config.kvTimeout(timeouts.getKeyValue())
			.analyticsTimeout(timeouts.getAnalytics())
			.kvDurableTimeout(timeouts.getKeyValueDurable())
			.queryTimeout(timeouts.getQuery())
			.viewTimeout(timeouts.getView())
			.searchTimeout(timeouts.getSearch())
			.managementTimeout(timeouts.getManagement())
			.connectTimeout(timeouts.getConnect())
			.disconnectTimeout(timeouts.getDisconnect()));
		CouchbaseProperties.Io io = this.properties.getEnv().getIo();
		builder.ioConfig((config) -> config.maxHttpConnections(io.getMaxEndpoints())
			.numKvConnections(io.getMinEndpoints())
			.idleHttpConnectionTimeout(io.getIdleHttpConnectionTimeout()));
		if (this.properties.getEnv().getSsl().getEnabled()) {
			configureSsl(builder, sslBundles);
		}
		return builder;
	}

	/**
     * Configures SSL for the Couchbase client.
     * 
     * @param builder The builder used to configure the Couchbase client.
     * @param sslBundles The SSL bundles used for configuring SSL options.
     */
    private void configureSsl(Builder builder, SslBundles sslBundles) {
		Ssl sslProperties = this.properties.getEnv().getSsl();
		SslBundle sslBundle = (StringUtils.hasText(sslProperties.getBundle()))
				? sslBundles.getBundle(sslProperties.getBundle()) : null;
		Assert.state(sslBundle == null || !sslBundle.getOptions().isSpecified(),
				"SSL Options cannot be specified with Couchbase");
		builder.securityConfig((config) -> {
			config.enableTls(true);
			TrustManagerFactory trustManagerFactory = getTrustManagerFactory(sslBundle);
			if (trustManagerFactory != null) {
				config.trustManagerFactory(trustManagerFactory);
			}
		});
	}

	/**
     * Returns the TrustManagerFactory from the given SslBundle.
     * 
     * @param sslBundle the SslBundle containing the TrustManagerFactory
     * @return the TrustManagerFactory from the SslBundle, or null if the SslBundle is null
     */
    private TrustManagerFactory getTrustManagerFactory(SslBundle sslBundle) {
		return (sslBundle != null) ? sslBundle.getManagers().getTrustManagerFactory() : null;
	}

	/**
     * JacksonConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ObjectMapper.class)
	static class JacksonConfiguration {

		/**
         * Returns a customizer for the ClusterEnvironmentBuilder that configures it with a Jackson ObjectMapper.
         * This customizer is only applied if there is a single candidate for the ObjectMapper bean.
         * The customizer creates a new instance of JacksonClusterEnvironmentBuilderCustomizer with a modified ObjectMapper
         * that registers the JsonValueModule module.
         *
         * @param objectMapper the ObjectMapper bean to be used for customizing the ClusterEnvironmentBuilder
         * @return a customizer for the ClusterEnvironmentBuilder that configures it with the modified ObjectMapper
         */
        @Bean
		@ConditionalOnSingleCandidate(ObjectMapper.class)
		ClusterEnvironmentBuilderCustomizer jacksonClusterEnvironmentBuilderCustomizer(ObjectMapper objectMapper) {
			return new JacksonClusterEnvironmentBuilderCustomizer(
					objectMapper.copy().registerModule(new JsonValueModule()));
		}

	}

	/**
     * JacksonClusterEnvironmentBuilderCustomizer class.
     */
    private static final class JacksonClusterEnvironmentBuilderCustomizer
			implements ClusterEnvironmentBuilderCustomizer, Ordered {

		private final ObjectMapper objectMapper;

		/**
         * Constructs a new JacksonClusterEnvironmentBuilderCustomizer with the specified ObjectMapper.
         *
         * @param objectMapper the ObjectMapper to be used by the customizer
         */
        private JacksonClusterEnvironmentBuilderCustomizer(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		/**
         * Customizes the JacksonClusterEnvironmentBuilder by configuring the JSON serializer with the provided ObjectMapper.
         *
         * @param builder the JacksonClusterEnvironmentBuilder to be customized
         */
        @Override
		public void customize(Builder builder) {
			builder.jsonSerializer(JacksonJsonSerializer.create(this.objectMapper));
		}

		/**
         * Returns the order in which this customizer should be applied.
         * 
         * @return the order value
         */
        @Override
		public int getOrder() {
			return 0;
		}

	}

	/**
	 * Condition that matches when {@code spring.couchbase.connection-string} has been
	 * configured or there is a {@link CouchbaseConnectionDetails} bean.
	 */
	static final class CouchbaseCondition extends AnyNestedCondition {

		/**
         * Constructs a new instance of CouchbaseCondition.
         * 
         * @param phase the configuration phase of the condition
         */
        CouchbaseCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		/**
         * CouchbaseUrlCondition class.
         */
        @ConditionalOnProperty(prefix = "spring.couchbase", name = "connection-string")
		private static final class CouchbaseUrlCondition {

		}

		/**
         * CouchbaseConnectionDetailsCondition class.
         */
        @ConditionalOnBean(CouchbaseConnectionDetails.class)
		private static final class CouchbaseConnectionDetailsCondition {

		}

	}

	/**
	 * Adapts {@link CouchbaseProperties} to {@link CouchbaseConnectionDetails}.
	 */
	static final class PropertiesCouchbaseConnectionDetails implements CouchbaseConnectionDetails {

		private final CouchbaseProperties properties;

		/**
         * Initializes a new instance of the PropertiesCouchbaseConnectionDetails class with the specified CouchbaseProperties.
         * 
         * @param properties The CouchbaseProperties object containing the connection details.
         */
        PropertiesCouchbaseConnectionDetails(CouchbaseProperties properties) {
			this.properties = properties;
		}

		/**
         * Returns the connection string for the Couchbase database.
         * 
         * @return the connection string
         */
        @Override
		public String getConnectionString() {
			return this.properties.getConnectionString();
		}

		/**
         * Returns the username associated with the Couchbase connection details.
         *
         * @return the username associated with the Couchbase connection details
         */
        @Override
		public String getUsername() {
			return this.properties.getUsername();
		}

		/**
         * Returns the password for the Couchbase connection.
         * 
         * @return the password for the Couchbase connection
         */
        @Override
		public String getPassword() {
			return this.properties.getPassword();
		}

	}

}

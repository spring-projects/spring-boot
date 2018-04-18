/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.cassandra.embedded;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.Cluster;
import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.EncryptionOptions;
import org.apache.cassandra.config.ParameterizedClass;
import org.apache.cassandra.service.CassandraDaemon;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.data.cassandra.config.CassandraClusterFactoryBean;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.SocketUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Embedded Cassandra.
 *
 * @author Dmytro Nosan
 */
@Configuration
@EnableConfigurationProperties({EmbeddedCassandraProperties.class})
@AutoConfigureBefore(CassandraAutoConfiguration.class)
@ConditionalOnClass({CassandraDaemon.class, Config.class, Yaml.class})
public class EmbeddedCassandraAutoConfiguration {

	private final EmbeddedCassandraProperties embeddedProperties;
	private final List<ConfigCustomizer> configCustomizers;
	private final ApplicationContext context;

	public EmbeddedCassandraAutoConfiguration(EmbeddedCassandraProperties embeddedProperties,
			ObjectProvider<List<ConfigCustomizer>> configCustomizers,
			ApplicationContext context) {
		this.embeddedProperties = embeddedProperties;
		this.configCustomizers = configCustomizers.getIfAvailable();
		this.context = context;
	}


	@Bean(initMethod = "start", destroyMethod = "stop", name = "embeddedCassandraServer")
	public EmbeddedCassandraServer embeddedCassandraServer(Config config) {
		PropertyMapper mapper = PropertyMapper.get();
		EmbeddedCassandraServer cassandra = new EmbeddedCassandraServer();
		EmbeddedCassandraProperties props = this.embeddedProperties;
		mapper.from(props::getWorkingDirectory).whenNonNull().as(Paths::get).to(cassandra::setWorkingDir);
		mapper.from(props::getStartupTimeout).whenNonNull().to(cassandra::setStartupTimeout);
		mapper.from(props::getArguments).when(it -> !CollectionUtils.isEmpty(it)).to(cassandra::setArgs);
		cassandra.setConfig(config);
		setPortProperty(this.context, config.native_transport_port);
		return cassandra;
	}


	@Bean
	@ConditionalOnMissingBean
	public Config embeddedCassandraConfig() {
		PropertyMapper mapper = PropertyMapper.get();
		Iterator<Integer> ports = SocketUtils.findAvailableTcpPorts(5).iterator();
		EmbeddedCassandraProperties props = this.embeddedProperties;
		Config config = new Config();

		mapper.from(props::getClusterName).whenHasText().to(it -> config.cluster_name = it);
		mapper.from(props::getPort).as(it -> randomPort(it, ports)).to(it -> {
			config.native_transport_port = it;
			config.start_native_transport = true;
		});
		mapper.from(props::getListenAddress).whenHasText()
				.to(it -> config.listen_address = it);
		mapper.from(props::getListenInterface).whenHasText()
				.to(it -> config.listen_address = it);
		mapper.from(props::isListenInterfacePreferIpv6)
				.to(it -> config.listen_interface_prefer_ipv6 = it);
		mapper.from(props::getPortSsl).whenNonNull().as(it -> randomPort(it, ports))
				.to(it -> config.native_transport_port_ssl = it);
		mapper.from(props::getRpcPort).as(it -> randomPort(it, ports)).to(it -> {
			config.rpc_port = it;
			config.start_rpc = true;
		});
		mapper.from(props::getRpcAddress).whenHasText()
				.to(it -> config.rpc_address = it);
		mapper.from(props::getRpcInterface).whenHasText()
				.to(it -> config.rpc_interface = it);
		mapper.from(props::isRpcInterfacePreferIpv6)
				.to(it -> config.rpc_interface_prefer_ipv6 = it);
		mapper.from(props::isRpcKeepalive).to(it -> config.rpc_keepalive = it);
		mapper.from(props::getStoragePort).as(it -> randomPort(it, ports)).to(it -> config.storage_port = it);
		mapper.from(props::getStorageSslPort).as(it -> randomPort(it, ports))
				.to(it -> config.ssl_storage_port = it);
		mapper.from(props::getHintsDirectory).whenNonNull().to(it -> config.hints_directory = it);
		mapper.from(props::getHintsFlushPeriod).whenNonNull().to(it -> config.hints_flush_period_in_ms =
				Math.toIntExact(it.toMillis()));
		mapper.from(props::getSavedCachesDirectory).whenNonNull().to(it -> config.saved_caches_directory = it);
		mapper.from(props::getCommitLogDirectory).whenNonNull().to(it -> config.commitlog_directory = it);
		mapper.from(props::getCdcRawDirectory).whenNonNull().to(it -> config.cdc_raw_directory = it);
		mapper.from(props::isCdcEnabled).to(it -> config.cdc_enabled = it);
		mapper.from(props::getDataDirectories).as(it -> it.toArray(new String[0]))
				.to(it -> config.data_file_directories = it);
		mapper.from(props::getCommitLogSync).whenNonNull().to(it -> config.commitlog_sync = it);
		mapper.from(props::getCommitLogSyncBatch).whenNonNull()
				.to(it -> config.commitlog_sync_batch_window_in_ms = it.toMillis());
		mapper.from(props::getCommitLogSyncPeriod).whenNonNull()
				.to(it -> config.commitlog_sync_period_in_ms = Math.toIntExact(it.toMillis()));
		mapper.from(props::getPartitioner).whenNonNull().to(it -> config.partitioner = it.getCanonicalName());
		mapper.from(props::getAuthenticator).whenNonNull().to(it -> config.authenticator = it.getCanonicalName());
		mapper.from(props::getAuthorizer).whenNonNull().to(it -> config.authorizer = it.getCanonicalName());
		mapper.from(props::getRoleManager).whenNonNull().to(it -> config.role_manager = it.getCanonicalName());
		mapper.from(props::getInternodeAuthenticator).whenNonNull()
				.to(it -> config.internode_authenticator = it.getCanonicalName());
		mapper.from(props::getRequestScheduler).whenNonNull().to(it -> config.request_scheduler = it.getCanonicalName());
		mapper.from(props::getSeedProvider).as(this::toParameterizedClass).whenNonNull()
				.to(it -> config.seed_provider = it);
		mapper.from(props::getEndpointSnitch).whenNonNull().to(it -> config.endpoint_snitch = it.getCanonicalName());
		mapper.from(props::getDiskFailurePolicy).whenNonNull().to(it -> config.disk_failure_policy = it);
		mapper.from(props::getMemtableAllocationType).whenNonNull().to(it -> config.memtable_allocation_type = it);
		mapper.from(props::getInternodeCompression).whenNonNull().to(it -> config.internode_compression = it);
		mapper.from(props::getDiskAccessMode).whenNonNull().to(it -> config.disk_access_mode = it);
		mapper.from(props::getCommitFailurePolicy).whenNonNull().to(it -> config.commit_failure_policy = it);
		mapper.from(props::getUserFunctionTimeoutPolicy).whenNonNull().to(it -> config.user_function_timeout_policy = it);
		mapper.from(props::getRequestSchedulerId).whenNonNull().to(it -> config.request_scheduler_id = it);
		mapper.from(props::getDiskOptimizationStrategy).whenNonNull().to(it -> config.disk_optimization_strategy = it);
		mapper.from(props::getRpcType).whenHasText().to(it -> config.rpc_server_type = it);
		mapper.from(props::getCommitLogCompression).as(this::toParameterizedClass).whenNonNull()
				.to(it -> config.commitlog_compression = it);
		mapper.from(props::getPermissionsValidity).whenNonNull()
				.to(it -> config.permissions_validity_in_ms = Math.toIntExact(it.toMillis()));
		mapper.from(props::getPermissionsUpdateInterval).whenNonNull()
				.to(it -> config.permissions_update_interval_in_ms = Math.toIntExact(it.toMillis()));
		mapper.from(props::getRolesValidity).whenNonNull()
				.to(it -> config.roles_validity_in_ms = Math.toIntExact(it.toMillis()));
		mapper.from(props::getRolesUpdateInterval).whenNonNull()
				.to(it -> config.roles_update_interval_in_ms = Math.toIntExact(it.toMillis()));
		mapper.from(props::getCredentialsValidity).whenNonNull()
				.to(it -> config.credentials_validity_in_ms = Math.toIntExact(it.toMillis()));
		mapper.from(props::getCredentialsUpdateInterval).whenNonNull()
				.to(it -> config.credentials_update_interval_in_ms = Math.toIntExact(it.toMillis()));
		mapper.from(props::getHintsCompression).as(this::toParameterizedClass).whenNonNull()
				.to(it -> config.hints_compression = it);
		mapper.from(props::getRequestTimeout).whenNonNull().to(it -> config.request_timeout_in_ms = it.toMillis());
		mapper.from(props::getReadRequestTimeout).whenNonNull()
				.to(it -> config.read_request_timeout_in_ms = it.toMillis());
		mapper.from(props::getRangeRequestTimeout).whenNonNull()
				.to(it -> config.range_request_timeout_in_ms = it.toMillis());
		mapper.from(props::getWriteRequestTimeout).whenNonNull()
				.to(it -> config.write_request_timeout_in_ms = it.toMillis());
		mapper.from(props::getCounterWriteRequestTimeout).whenNonNull()
				.to(it -> config.counter_write_request_timeout_in_ms = it.toMillis());
		mapper.from(props::getCasContentionTimeout).whenNonNull()
				.to(it -> config.cas_contention_timeout_in_ms = it.toMillis());
		mapper.from(props::getTruncateRequestTimeout).whenNonNull()
				.to(it -> config.truncate_request_timeout_in_ms = it.toMillis());
		mapper.from(props::getSlowQueryLogTimeout).whenNonNull()
				.to(it -> config.slow_query_log_timeout_in_ms = it.toMillis());
		mapper.from(props::getConcurrentReads).to(it -> config.concurrent_reads = it);
		mapper.from(props::getConcurrentWrites).to(it -> config.concurrent_writes = it);
		mapper.from(props::getConcurrentCounterWrites).to(it -> config.concurrent_counter_writes = it);
		mapper.from(props::getConcurrentMaterializedViewWrites)
				.to(it -> config.concurrent_materialized_view_writes = it);
		mapper.from(props::getBroadcastAddress).whenHasText()
				.to(it -> config.broadcast_address = it);
		mapper.from(props::getBroadcastRpcAddress).whenHasText()
				.to(it -> config.broadcast_rpc_address = it);
		mapper.from(props::isListenOnBroadcastAddress).to(it -> config.listen_on_broadcast_address = it);
		mapper.from(props::isAutoSnapshot).to(it -> config.auto_snapshot = it);
		mapper.from(props::getBackPressureStrategy).as(this::toParameterizedClass).whenNonNull()
				.to(it -> {
					config.back_pressure_enabled = true;
					config.back_pressure_strategy = it;
				});
		mapper.from(props::getClientEncryption).as(this::toClientEncryptionOptions).whenNonNull()
				.to(it -> config.client_encryption_options = it);
		mapper.from(props::getServerEncryption).as(this::toServerEncryptionOptions).whenNonNull()
				.to(it -> config.server_encryption_options = it);

		customize(config);

		return config;
	}


	private void customize(Config config) {
		if (!CollectionUtils.isEmpty(this.configCustomizers)) {
			ArrayList<ConfigCustomizer> configCustomizers = new ArrayList<>(this.configCustomizers);
			AnnotationAwareOrderComparator.sort(configCustomizers);
			for (ConfigCustomizer configCustomizer : configCustomizers) {
				configCustomizer.customize(config);
			}

		}
	}

	private EncryptionOptions.ClientEncryptionOptions toClientEncryptionOptions(EmbeddedCassandraProperties
			.ClientEncryption encryption) {
		if (encryption == null) {
			return null;
		}
		EncryptionOptions.ClientEncryptionOptions options = new EncryptionOptions.ClientEncryptionOptions();
		PropertyMapper mapper = PropertyMapper.get();
		mapper.from(encryption::getCipherSuites).as(it -> it.toArray(new String[0]))
				.when(it -> !ObjectUtils.isEmpty(it))
				.to(it -> options.cipher_suites = it);
		mapper.from(encryption::getAlgorithm).whenHasText().to(it -> options.algorithm = it);
		mapper.from(encryption::getKeystore).whenHasText().to(it -> options.keystore = it);
		mapper.from(encryption::getKeystorePassword).whenHasText().to(it -> options.keystore_password = it);
		mapper.from(encryption::getStoreType).whenHasText().to(it -> options.store_type = it);
		mapper.from(encryption::getProtocol).whenHasText().to(it -> options.protocol = it);
		mapper.from(encryption::getTruststore).whenHasText().to(it -> options.truststore = it);
		mapper.from(encryption::getTruststorePassword).whenHasText().to(it -> options.truststore_password = it);
		mapper.from(encryption::isRequireClientAuth).to(it -> options.require_client_auth = it);
		mapper.from(encryption::isRequireEndpointVerification).to(it -> options.require_endpoint_verification = it);
		mapper.from(encryption::isEnabled).to(it -> options.enabled = it);
		mapper.from(encryption::isOptional).to(it -> options.optional = it);
		return options;
	}

	private EncryptionOptions.ServerEncryptionOptions toServerEncryptionOptions(EmbeddedCassandraProperties
			.ServerEncryption encryption) {
		if (encryption == null) {
			return null;
		}
		PropertyMapper mapper = PropertyMapper.get();
		EncryptionOptions.ServerEncryptionOptions options = new EncryptionOptions.ServerEncryptionOptions();
		mapper.from(encryption::getCipherSuites).as(it -> it.toArray(new String[0]))
				.when(it -> !ObjectUtils.isEmpty(it))
				.to(it -> options.cipher_suites = it);
		mapper.from(encryption::getAlgorithm).whenHasText().to(it -> options.algorithm = it);
		mapper.from(encryption::getKeystore).whenHasText().to(it -> options.keystore = it);
		mapper.from(encryption::getKeystorePassword).whenHasText().to(it -> options.keystore_password = it);
		mapper.from(encryption::getStoreType).whenHasText().to(it -> options.store_type = it);
		mapper.from(encryption::getProtocol).whenHasText().to(it -> options.protocol = it);
		mapper.from(encryption::getTruststore).whenHasText().to(it -> options.truststore = it);
		mapper.from(encryption::getTruststorePassword).whenHasText().to(it -> options.truststore_password = it);
		mapper.from(encryption::getInternodeEncryption).whenNonNull().to(it -> options.internode_encryption = it);
		mapper.from(encryption::isRequireClientAuth).to(it -> options.require_client_auth = it);
		mapper.from(encryption::isRequireEndpointVerification).to(it -> options.require_endpoint_verification = it);
		return options;
	}

	private ParameterizedClass toParameterizedClass(EmbeddedCassandraProperties.ComplexClass<?> complexClass) {
		if (complexClass != null && complexClass.getTargetClass() != null) {
			return new ParameterizedClass(complexClass.getTargetClass().getCanonicalName(),
					complexClass.getParameters());
		}
		return null;
	}

	private int randomPort(int port, Iterator<Integer> ports) {
		if (port == 0) {
			return ports.next();
		}
		return port;
	}

	private void setPortProperty(ApplicationContext context, int port) {
		if (context instanceof ConfigurableApplicationContext) {
			MutablePropertySources sources = ((ConfigurableApplicationContext) context)
					.getEnvironment().getPropertySources();
			getCassandraPorts(sources).put("local.cassandra.port", port);
		}
		if (context.getParent() != null) {
			setPortProperty(context.getParent(), port);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getCassandraPorts(MutablePropertySources sources) {
		PropertySource<?> propertySource = sources.get("cassandra.ports");
		if (propertySource == null) {
			propertySource = new MapPropertySource("cassandra.ports", new HashMap<>());
			sources.addFirst(propertySource);
		}
		return (Map<String, Object>) propertySource.getSource();
	}


	/**
	 * Additional configuration to ensure that {@link Cluster} bean depends on the
	 * {@code embeddedCassandraServer} bean.
	 */
	@Configuration
	@ConditionalOnClass({Cluster.class, CassandraClusterFactoryBean.class})
	protected static class EmbeddedCassandraDependencyConfiguration extends AbstractDependsOnBeanFactoryPostProcessor {

		public EmbeddedCassandraDependencyConfiguration() {
			super(Cluster.class, CassandraClusterFactoryBean.class, "embeddedCassandraServer");
		}

	}


}

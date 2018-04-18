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

import java.io.File;

import com.datastax.driver.core.Cluster;
import org.apache.cassandra.auth.AllowAllAuthenticator;
import org.apache.cassandra.auth.AllowAllAuthorizer;
import org.apache.cassandra.auth.AllowAllInternodeAuthenticator;
import org.apache.cassandra.auth.CassandraRoleManager;
import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.EncryptionOptions;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.io.compress.LZ4Compressor;
import org.apache.cassandra.locator.SimpleSeedProvider;
import org.apache.cassandra.locator.SimpleSnitch;
import org.apache.cassandra.net.RateBasedBackPressure;
import org.apache.cassandra.scheduler.NoScheduler;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedCassandraAutoConfiguration}.
 *
 * @author Dmytro Nosan
 */
public class EmbeddedCassandraAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;
	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}


	@Test
	public void defaultConfig() {
		load();

		assertThat(this.context.getBeansOfType(Config.class)).hasSize(1);
		Config config = this.context.getBean(Config.class);

		assertThat(config.cluster_name).isEqualTo("Test Cluster");
		assertThat(config.native_transport_port).isNotEqualTo(0);
		assertThat(config.rpc_port).isNotEqualTo(0);
		assertThat(config.storage_port).isNotEqualTo(0);
		assertThat(config.ssl_storage_port).isNotEqualTo(0);
		assertThat(config.native_transport_port_ssl).isNull();
		assertThat(config.hints_directory).isNull();
		assertThat(config.saved_caches_directory).isNull();
		assertThat(config.commitlog_directory).isNull();
		assertThat(config.cdc_raw_directory).isNull();
		assertThat(config.data_file_directories).isEmpty();
		assertThat(config.commitlog_sync).isEqualTo(Config.CommitLogSync.periodic);
		assertThat(config.commitlog_sync_period_in_ms).isEqualTo(10000);
		assertThat(config.commitlog_sync_batch_window_in_ms).isEqualTo(Double.NaN);
		assertThat(config.partitioner).isEqualTo(Murmur3Partitioner.class.getCanonicalName());
		assertThat(config.seed_provider.class_name).isEqualTo(SimpleSeedProvider.class.getCanonicalName());
		assertThat(config.seed_provider.parameters).containsEntry("seeds", "localhost");
		assertThat(config.endpoint_snitch).isEqualTo(SimpleSnitch.class.getCanonicalName());
		assertThat(config.request_scheduler).isEqualTo(NoScheduler.class.getCanonicalName());
		assertThat(config.internode_authenticator).isEqualTo(AllowAllInternodeAuthenticator.class.getCanonicalName());
		assertThat(config.authenticator).isEqualTo(AllowAllAuthenticator.class.getCanonicalName());
		assertThat(config.authorizer).isEqualTo(AllowAllAuthorizer.class.getCanonicalName());
		assertThat(config.role_manager).isEqualTo(CassandraRoleManager.class.getCanonicalName());
		assertThat(config.disk_failure_policy).isEqualTo(Config.DiskFailurePolicy.ignore);
		assertThat(config.disk_access_mode).isEqualTo(Config.DiskAccessMode.auto);
		assertThat(config.commit_failure_policy).isEqualTo(Config.CommitFailurePolicy.stop);
		assertThat(config.user_function_timeout_policy).isEqualTo(Config.UserFunctionTimeoutPolicy.die);
		assertThat(config.request_scheduler_id).isEqualTo(Config.RequestSchedulerId.keyspace);
		assertThat(config.disk_optimization_strategy).isEqualTo(Config.DiskOptimizationStrategy.ssd);
		assertThat(config.internode_compression).isEqualTo(Config.InternodeCompression.none);
		assertThat(config.memtable_allocation_type).isEqualTo(Config.MemtableAllocationType.heap_buffers);
		assertThat(config.commitlog_compression).isNull();
		assertThat(config.hints_compression).isNull();
		assertThat(config.hints_flush_period_in_ms).isEqualTo(10000);
		assertThat(config.back_pressure_strategy).isNull();
		assertThat(config.back_pressure_enabled).isFalse();
		assertThat(config.permissions_validity_in_ms).isEqualTo(2000);
		assertThat(config.permissions_update_interval_in_ms).isEqualTo(2000);
		assertThat(config.roles_validity_in_ms).isEqualTo(2000);
		assertThat(config.roles_update_interval_in_ms).isEqualTo(2000);
		assertThat(config.credentials_validity_in_ms).isEqualTo(2000);
		assertThat(config.credentials_update_interval_in_ms).isEqualTo(2000);
		assertThat(config.request_timeout_in_ms).isEqualTo(10000);
		assertThat(config.read_request_timeout_in_ms).isEqualTo(5000);
		assertThat(config.range_request_timeout_in_ms).isEqualTo(10000);
		assertThat(config.write_request_timeout_in_ms).isEqualTo(2000);
		assertThat(config.counter_write_request_timeout_in_ms).isEqualTo(5000);
		assertThat(config.cas_contention_timeout_in_ms).isEqualTo(1000);
		assertThat(config.truncate_request_timeout_in_ms).isEqualTo(60000);
		assertThat(config.slow_query_log_timeout_in_ms).isEqualTo(500);
		assertThat(config.concurrent_reads).isEqualTo(32);
		assertThat(config.concurrent_writes).isEqualTo(32);
		assertThat(config.concurrent_counter_writes).isEqualTo(32);
		assertThat(config.concurrent_materialized_view_writes).isEqualTo(32);
		assertThat(config.listen_interface_prefer_ipv6).isFalse();
		assertThat(config.broadcast_address).isNull();
		assertThat(config.broadcast_rpc_address).isNull();
		assertThat(config.listen_on_broadcast_address).isFalse();
		assertThat(config.rpc_server_type).isEqualTo("sync");
		assertThat(config.start_native_transport).isTrue();
		assertThat(config.start_rpc).isTrue();
		assertThat(config.rpc_interface).isNull();
		assertThat(config.rpc_interface_prefer_ipv6).isFalse();
		assertThat(config.rpc_keepalive).isTrue();
		assertThat(config.auto_snapshot).isTrue();
		assertThat(config.cdc_enabled).isFalse();

		assertThat(config.server_encryption_options).isNotNull();
		assertThat(config.server_encryption_options.algorithm).isEqualTo("SunX509");
		assertThat(config.server_encryption_options.cipher_suites).contains("TLS_RSA_WITH_AES_256_CBC_SHA");
		assertThat(config.server_encryption_options.keystore).isEqualTo("conf/.keystore");
		assertThat(config.server_encryption_options.keystore_password).isEqualTo("cassandra");
		assertThat(config.server_encryption_options.protocol).isEqualTo("TLS");
		assertThat(config.server_encryption_options.require_client_auth).isFalse();
		assertThat(config.server_encryption_options.require_endpoint_verification).isFalse();
		assertThat(config.server_encryption_options.truststore).isEqualTo("conf/.truststore");
		assertThat(config.server_encryption_options.truststore_password).isEqualTo("cassandra");
		assertThat(config.server_encryption_options.internode_encryption).isEqualTo(EncryptionOptions
				.ServerEncryptionOptions.InternodeEncryption.none);
		assertThat(config.server_encryption_options.store_type).isEqualTo("JKS");


		assertThat(config.client_encryption_options).isNotNull();
		assertThat(config.client_encryption_options.algorithm).isEqualTo("SunX509");
		assertThat(config.client_encryption_options.cipher_suites).contains("TLS_RSA_WITH_AES_256_CBC_SHA");
		assertThat(config.client_encryption_options.keystore).isEqualTo("conf/.keystore");
		assertThat(config.client_encryption_options.keystore_password).isEqualTo("cassandra");
		assertThat(config.client_encryption_options.protocol).isEqualTo("TLS");
		assertThat(config.client_encryption_options.require_client_auth).isFalse();
		assertThat(config.client_encryption_options.require_endpoint_verification).isFalse();
		assertThat(config.client_encryption_options.truststore).isEqualTo("conf/.truststore");
		assertThat(config.client_encryption_options.truststore_password).isEqualTo("cassandra");
		assertThat(config.client_encryption_options.store_type).isEqualTo("JKS");
		assertThat(config.client_encryption_options.optional).isFalse();
		assertThat(config.client_encryption_options.enabled).isFalse();

	}

	@Test
	public void setProperties() {
		load("spring.cassandra.embedded.cluster-name=Boot Cluster",
				"spring.cassandra.embedded.startup-timeout=0s",
				"spring.cassandra.embedded.port=2000",
				"spring.cassandra.embedded.listen-address=127.0.0.1",
				"spring.cassandra.embedded.listen-interface=eth0",
				"spring.cassandra.embedded.port-ssl=2001",
				"spring.cassandra.embedded.storage-port=2002",
				"spring.cassandra.embedded.storage-ssl-port=20003",
				"spring.cassandra.embedded.hints-directory=/hints",
				"spring.cassandra.embedded.saved-caches-directory=/caches",
				"spring.cassandra.embedded.commit-log-directory=/commitlog",
				"spring.cassandra.embedded.cdc-raw-directory=/cdc",
				"spring.cassandra.embedded.data-directories=/dir1,/dir2",
				"spring.cassandra.embedded.commit-log-sync=periodic",
				"spring.cassandra.embedded.commit-log-sync-batch=1s",
				"spring.cassandra.embedded.commit-log-sync-period=2s",
				"spring.cassandra.embedded.partitioner=org.apache.cassandra.dht.Murmur3Partitioner",
				"spring.cassandra.embedded.seed-provider.target-class=org.apache.cassandra.locator.SimpleSeedProvider",
				"spring.cassandra.embedded.seed-provider.parameters.seeds=127.0.0.1",
				"spring.cassandra.embedded.endpoint-snitch=org.apache.cassandra.locator.SimpleSnitch",
				"spring.cassandra.embedded.request-scheduler=org.apache.cassandra.scheduler.NoScheduler",
				"spring.cassandra.embedded.internode-authenticator=org.apache.cassandra.auth.AllowAllInternodeAuthenticator",
				"spring.cassandra.embedded.authenticator=org.apache.cassandra.auth.AllowAllAuthenticator",
				"spring.cassandra.embedded.authorizer=org.apache.cassandra.auth.AllowAllAuthorizer",
				"spring.cassandra.embedded.role-manager=org.apache.cassandra.auth.CassandraRoleManager",
				"spring.cassandra.embedded.disk-failure-policy=die",
				"spring.cassandra.embedded.disk-access-mode=standard",
				"spring.cassandra.embedded.commit-failure-policy=die",
				"spring.cassandra.embedded.user-function-timeout-policy=die",
				"spring.cassandra.embedded.request-scheduler-id=keyspace",
				"spring.cassandra.embedded.disk-optimization-strategy=spinning",
				"spring.cassandra.embedded.internode-compression=all",
				"spring.cassandra.embedded.memtable-allocation-type=heap_buffers",
				"spring.cassandra.embedded.commit-log-compression.target-class=org.apache.cassandra.io.compress.LZ4Compressor",
				"spring.cassandra.embedded.hints-compression.target-class=org.apache.cassandra.io.compress.LZ4Compressor",
				"spring.cassandra.embedded.back-pressure-strategy.target-class=org.apache.cassandra.net.RateBasedBackPressure",
				"spring.cassandra.embedded.permissions-validity=1s",
				"spring.cassandra.embedded.permissions-update-interval=2s",
				"spring.cassandra.embedded.roles-validity=3s",
				"spring.cassandra.embedded.roles-update-interval=4s",
				"spring.cassandra.embedded.credentials-validity=5s",
				"spring.cassandra.embedded.credentials-update-interval=6s",
				"spring.cassandra.embedded.request-timeout=7s",
				"spring.cassandra.embedded.read-request-timeout=8s",
				"spring.cassandra.embedded.range-request-timeout=9s",
				"spring.cassandra.embedded.write-request-timeout=10s",
				"spring.cassandra.embedded.counter-write-request-timeout=11s",
				"spring.cassandra.embedded.cas-contention-timeout=12s",
				"spring.cassandra.embedded.truncate-request-timeout=13s",
				"spring.cassandra.embedded.hints-flush-period=14s",
				"spring.cassandra.embedded.slow-query-log-timeout=200ms",
				"spring.cassandra.embedded.concurrent-reads=16",
				"spring.cassandra.embedded.concurrent-writes=17",
				"spring.cassandra.embedded.concurrent-counter-writes=18",
				"spring.cassandra.embedded.concurrent-materialized-view-writes=19",
				"spring.cassandra.embedded.listen-interface-prefer-ipv6=true",
				"spring.cassandra.embedded.broadcast-address=127.0.0.1",
				"spring.cassandra.embedded.broadcast-rpc-address=127.0.0.1",
				"spring.cassandra.embedded.listen-on-broadcast-address=true",
				"spring.cassandra.embedded.rpc-type=sync",
				"spring.cassandra.embedded.rpc-port=2009",
				"spring.cassandra.embedded.rpc-address=127.0.0.1",
				"spring.cassandra.embedded.rpc-interface=eth0",
				"spring.cassandra.embedded.rpc-keepalive=false",
				"spring.cassandra.embedded.auto-snapshot=false",
				"spring.cassandra.embedded.rpc-interface-prefer-ipv6=true",
				"spring.cassandra.embedded.cdc-enabled=true",
				"spring.cassandra.embedded.arguments[0]=-agentlib:jdwp=transport=dt_socket,address=-1,server=y,suspend=y",
				"spring.cassandra.embedded.client-encryption.enabled=true",
				"spring.cassandra.embedded.client-encryption.require-endpoint-verification=true",
				"spring.cassandra.embedded.client-encryption.optional=false",
				"spring.cassandra.embedded.client-encryption.keystore=conf/keystore.node0",
				"spring.cassandra.embedded.client-encryption.keystore-password=cassandra",
				"spring.cassandra.embedded.client-encryption.require-client-auth=true",
				"spring.cassandra.embedded.client-encryption.truststore=conf/truststore.node0",
				"spring.cassandra.embedded.client-encryption.truststore-password=cassandra",
				"spring.cassandra.embedded.client-encryption.protocol=TLS",
				"spring.cassandra.embedded.client-encryption.algorithm=SunX509",
				"spring.cassandra.embedded.client-encryption.store-type=JKS",
				"spring.cassandra.embedded.client-encryption.cipher-suites=TLS_RSA_WITH_AES_256_CBC_SHA",
				"spring.cassandra.embedded.server-encryption.internode-encryption=all",
				"spring.cassandra.embedded.server-encryption.require-endpoint-verification=true",
				"spring.cassandra.embedded.server-encryption.keystore=conf/keystore.node0",
				"spring.cassandra.embedded.server-encryption.keystore-password=cassandra",
				"spring.cassandra.embedded.server-encryption.require-client-auth=true",
				"spring.cassandra.embedded.server-encryption.truststore=conf/truststore.node0",
				"spring.cassandra.embedded.server-encryption.truststore-password=cassandra",
				"spring.cassandra.embedded.server-encryption.protocol=TLS",
				"spring.cassandra.embedded.server-encryption.algorithm=SunX509",
				"spring.cassandra.embedded.server-encryption.store-type=JKS",
				"spring.cassandra.embedded.server-encryption.cipher-suites=TLS_RSA_WITH_AES_256_CBC_SHA"
		);

		assertThat(this.context.getBeansOfType(Config.class)).hasSize(1);
		Config config = this.context.getBean(Config.class);

		assertThat(config.cluster_name).isEqualTo("Boot Cluster");
		assertThat(config.native_transport_port).isEqualTo(2000);
		assertThat(config.rpc_port).isEqualTo(2009);
		assertThat(config.storage_port).isEqualTo(2002);
		assertThat(config.ssl_storage_port).isEqualTo(20003);
		assertThat(config.native_transport_port_ssl).isEqualTo(2001);
		assertThat(config.hints_directory).isEqualTo("/hints");
		assertThat(config.saved_caches_directory).isEqualTo("/caches");
		assertThat(config.commitlog_directory).isEqualTo("/commitlog");
		assertThat(config.cdc_raw_directory).isEqualTo("/cdc");
		assertThat(config.data_file_directories).contains("/dir1", "/dir2");
		assertThat(config.commitlog_sync).isEqualTo(Config.CommitLogSync.periodic);
		assertThat(config.commitlog_sync_period_in_ms).isEqualTo(2000);
		assertThat(config.commitlog_sync_batch_window_in_ms).isEqualTo(1000);
		assertThat(config.partitioner).isEqualTo(Murmur3Partitioner.class.getCanonicalName());
		assertThat(config.seed_provider.class_name).isEqualTo(SimpleSeedProvider.class.getCanonicalName());
		assertThat(config.seed_provider.parameters).containsEntry("seeds", "127.0.0.1");
		assertThat(config.endpoint_snitch).isEqualTo(SimpleSnitch.class.getCanonicalName());
		assertThat(config.request_scheduler).isEqualTo(NoScheduler.class.getCanonicalName());
		assertThat(config.internode_authenticator).isEqualTo(AllowAllInternodeAuthenticator.class.getCanonicalName());
		assertThat(config.authenticator).isEqualTo(AllowAllAuthenticator.class.getCanonicalName());
		assertThat(config.authorizer).isEqualTo(AllowAllAuthorizer.class.getCanonicalName());
		assertThat(config.role_manager).isEqualTo(CassandraRoleManager.class.getCanonicalName());
		assertThat(config.disk_failure_policy).isEqualTo(Config.DiskFailurePolicy.die);
		assertThat(config.disk_access_mode).isEqualTo(Config.DiskAccessMode.standard);
		assertThat(config.commit_failure_policy).isEqualTo(Config.CommitFailurePolicy.die);
		assertThat(config.user_function_timeout_policy).isEqualTo(Config.UserFunctionTimeoutPolicy.die);
		assertThat(config.request_scheduler_id).isEqualTo(Config.RequestSchedulerId.keyspace);
		assertThat(config.disk_optimization_strategy).isEqualTo(Config.DiskOptimizationStrategy.spinning);
		assertThat(config.internode_compression).isEqualTo(Config.InternodeCompression.all);
		assertThat(config.memtable_allocation_type).isEqualTo(Config.MemtableAllocationType.heap_buffers);
		assertThat(config.commitlog_compression.class_name).isEqualTo(LZ4Compressor.class.getCanonicalName());
		assertThat(config.hints_compression.class_name).isEqualTo(LZ4Compressor.class.getCanonicalName());
		assertThat(config.back_pressure_strategy.class_name).isEqualTo(RateBasedBackPressure.class.getCanonicalName());
		assertThat(config.back_pressure_enabled).isTrue();
		assertThat(config.permissions_validity_in_ms).isEqualTo(1000);
		assertThat(config.permissions_update_interval_in_ms).isEqualTo(2000);
		assertThat(config.roles_validity_in_ms).isEqualTo(3000);
		assertThat(config.roles_update_interval_in_ms).isEqualTo(4000);
		assertThat(config.credentials_validity_in_ms).isEqualTo(5000);
		assertThat(config.credentials_update_interval_in_ms).isEqualTo(6000);
		assertThat(config.request_timeout_in_ms).isEqualTo(7000);
		assertThat(config.read_request_timeout_in_ms).isEqualTo(8000);
		assertThat(config.range_request_timeout_in_ms).isEqualTo(9000);
		assertThat(config.write_request_timeout_in_ms).isEqualTo(10000);
		assertThat(config.counter_write_request_timeout_in_ms).isEqualTo(11000);
		assertThat(config.cas_contention_timeout_in_ms).isEqualTo(12000);
		assertThat(config.truncate_request_timeout_in_ms).isEqualTo(13000L);
		assertThat(config.hints_flush_period_in_ms).isEqualTo(14000L);
		assertThat(config.slow_query_log_timeout_in_ms).isEqualTo(200);
		assertThat(config.concurrent_reads).isEqualTo(16);
		assertThat(config.concurrent_writes).isEqualTo(17);
		assertThat(config.concurrent_counter_writes).isEqualTo(18);
		assertThat(config.concurrent_materialized_view_writes).isEqualTo(19);
		assertThat(config.listen_interface_prefer_ipv6).isTrue();
		assertThat(config.broadcast_address).isEqualTo("127.0.0.1");
		assertThat(config.broadcast_rpc_address).isEqualTo("127.0.0.1");
		assertThat(config.listen_on_broadcast_address).isTrue();
		assertThat(config.rpc_server_type).isEqualTo("sync");
		assertThat(config.start_native_transport).isTrue();
		assertThat(config.start_rpc).isTrue();
		assertThat(config.rpc_interface).isEqualTo("eth0");
		assertThat(config.rpc_interface_prefer_ipv6).isTrue();
		assertThat(config.rpc_keepalive).isFalse();
		assertThat(config.auto_snapshot).isFalse();
		assertThat(config.cdc_enabled).isTrue();

		assertThat(config.server_encryption_options).isNotNull();
		assertThat(config.server_encryption_options.algorithm).isEqualTo("SunX509");
		assertThat(config.server_encryption_options.cipher_suites).contains("TLS_RSA_WITH_AES_256_CBC_SHA");
		assertThat(config.server_encryption_options.keystore).isEqualTo("conf/keystore.node0");
		assertThat(config.server_encryption_options.keystore_password).isEqualTo("cassandra");
		assertThat(config.server_encryption_options.protocol).isEqualTo("TLS");
		assertThat(config.server_encryption_options.require_client_auth).isTrue();
		assertThat(config.server_encryption_options.require_endpoint_verification).isTrue();
		assertThat(config.server_encryption_options.truststore).isEqualTo("conf/truststore.node0");
		assertThat(config.server_encryption_options.truststore_password).isEqualTo("cassandra");
		assertThat(config.server_encryption_options.internode_encryption).isEqualTo(EncryptionOptions
				.ServerEncryptionOptions.InternodeEncryption.all);
		assertThat(config.server_encryption_options.store_type).isEqualTo("JKS");


		assertThat(config.client_encryption_options).isNotNull();
		assertThat(config.client_encryption_options.algorithm).isEqualTo("SunX509");
		assertThat(config.client_encryption_options.cipher_suites).contains("TLS_RSA_WITH_AES_256_CBC_SHA");
		assertThat(config.client_encryption_options.keystore).isEqualTo("conf/keystore.node0");
		assertThat(config.client_encryption_options.keystore_password).isEqualTo("cassandra");
		assertThat(config.client_encryption_options.protocol).isEqualTo("TLS");
		assertThat(config.client_encryption_options.require_client_auth).isTrue();
		assertThat(config.client_encryption_options.require_endpoint_verification).isTrue();
		assertThat(config.client_encryption_options.truststore).isEqualTo("conf/truststore.node0");
		assertThat(config.client_encryption_options.truststore_password).isEqualTo("cassandra");
		assertThat(config.client_encryption_options.store_type).isEqualTo("JKS");
		assertThat(config.client_encryption_options.optional).isFalse();
		assertThat(config.client_encryption_options.enabled).isTrue();

		assertThat(this.outputCapture.toString())
				.contains("-agentlib:jdwp=transport=dt_socket,address=-1,server=y,suspend=y");
	}

	@Test
	public void portIsAvailableInParentContext() {
		try (ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext()) {
			parent.refresh();
			this.context = new AnnotationConfigApplicationContext();
			this.context.setParent(parent);
			this.context.register(EmbeddedCassandraAutoConfiguration.class,
					ClusterConfiguration.class);
			this.context.refresh();
			assertThat(parent.getEnvironment().getProperty("local.cassandra.port"))
					.isNotNull();
		}
	}

	@Test
	public void useCustomWorkingDir() {
		File workingDir = new File("target/cassandra");
		FileSystemUtils.deleteRecursively(workingDir);
		load("spring.cassandra.embedded.working-directory=" + workingDir.getPath());
		assertThat(workingDir).isDirectory();
		assertThat(workingDir.listFiles()).isNotEmpty();
	}


	@Test
	public void useRandomPort() {
		load(ClusterConfiguration.class);
		assertThat(this.context.getBeansOfType(Cluster.class)).hasSize(1);
		Cluster cluster = this.context.getBean(Cluster.class);
		Integer cassandraPort = Integer.valueOf(this.context.getEnvironment().getProperty("local.cassandra.port"));
		assertThat(cluster.getConfiguration().getProtocolOptions().getPort()).isEqualTo(cassandraPort);

		cluster.connect();
	}

	@Test
	public void customCustomizer() {
		load(CustomCustomizerConfiguration.class);
		assertThat(this.context.getBeansOfType(Config.class)).hasSize(1);
		Config config = this.context.getBean(Config.class);
		assertThat(config.cluster_name).isEqualTo("Spring Boot Cluster");

	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... props) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		if (config != null) {
			ctx.register(config);
		}
		TestPropertyValues.of(props).applyTo(ctx);
		ctx.register(EmbeddedCassandraAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration
	static class ClusterConfiguration {

		@Bean
		public Cluster cluster(@Value("${local.cassandra.port}") int port) {
			return Cluster.builder().addContactPoint("localhost").withPort(port).build();
		}

	}


	@Configuration
	static class CustomCustomizerConfiguration {

		@Bean
		public ConfigCustomizer customizer() {
			return config -> config.cluster_name = "Spring Boot Cluster";
		}
	}
}

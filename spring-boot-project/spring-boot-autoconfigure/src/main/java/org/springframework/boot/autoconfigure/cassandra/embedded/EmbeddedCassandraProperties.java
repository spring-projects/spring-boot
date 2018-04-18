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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.auth.AllowAllAuthenticator;
import org.apache.cassandra.auth.AllowAllAuthorizer;
import org.apache.cassandra.auth.AllowAllInternodeAuthenticator;
import org.apache.cassandra.auth.CassandraRoleManager;
import org.apache.cassandra.auth.IAuthenticator;
import org.apache.cassandra.auth.IAuthorizer;
import org.apache.cassandra.auth.IInternodeAuthenticator;
import org.apache.cassandra.auth.IRoleManager;
import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.EncryptionOptions.ServerEncryptionOptions.InternodeEncryption;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.io.compress.ICompressor;
import org.apache.cassandra.locator.IEndpointSnitch;
import org.apache.cassandra.locator.SeedProvider;
import org.apache.cassandra.locator.SimpleSeedProvider;
import org.apache.cassandra.locator.SimpleSnitch;
import org.apache.cassandra.net.BackPressureStrategy;
import org.apache.cassandra.scheduler.IRequestScheduler;
import org.apache.cassandra.scheduler.NoScheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Embedded Cassandra.
 *  <a href="https://docs.datastax.com/en/cassandra/3.0/cassandra/configuration/configCassandra_yaml.html">See more</a>
 *
 * @author Dmytro Nosan
 * @see ConfigCustomizer
 */
@ConfigurationProperties(prefix = "spring.cassandra.embedded")
public class EmbeddedCassandraProperties {
	/**
	 * The name of the cluster. This is mainly used to prevent machines in
	 * one logical cluster from joining another.
	 */
	private String clusterName;
	/**
	 * Port for the CQL native transport to listen for clients on.
	 */
	private int port = 0;
	/**
	 * Address or interface to bind to and tell other Cassandra nodes to connect to.
	 * Set listenAddress OR listenInterface, not both.
	 */
	private String listenAddress = "localhost";
	/**
	 * Set listenAddress OR listenInterface, not both. Interfaces must correspond to a single address, IP aliasing is not supported.
	 */
	private String listenInterface;
	/**
	 * Enabling native transport encryption.
	 */
	private Integer portSsl;
	/**
	 * TCP port, for commands and data.
	 */
	private int storagePort = 0;
	/**
	 * SSL port, for encrypted communication.
	 */
	private int storageSslPort = 0;
	/**
	 * Directory where Cassandra should store hints.
	 */
	private String hintsDirectory;
	/**
	 * The directory location where table key and row caches are stored.
	 */
	private String savedCachesDirectory;
	/**
	 * The directory where the commit log is stored.
	 */
	private String commitLogDirectory;
	/**
	 * The directory where the CDC log is stored.
	 */
	private String cdcRawDirectory;
	/**
	 * Directories where Cassandra should store data on disk.
	 */
	private final List<String> dataDirectories = new ArrayList<>();
	/**
	 * Policy for commit log sync.
	 */
	private Config.CommitLogSync commitLogSync = Config.CommitLogSync.periodic;
	/**
	 * When in batch mode, Cassandra won’t ack writes until the commit log has been fsynced to disk.
	 * It will wait commitLogSyncBatch between fsyncs
	 */
	private Duration commitLogSyncBatch;
	/**
	 * When in periodic mode, the CommitLog is simply synced every commitLogSyncPeriod.
	 */
	private Duration commitLogSyncPeriod = Duration.ofSeconds(10);
	/**
	 * The partitioner is responsible for distributing groups of rows (by partition key) across nodes in the cluster.
	 * <p>
	 * Besides Murmur3Partitioner, partitioner included for backwards compatibility include RandomPartitioner,
	 * ByteOrderedPartitioner, and OrderPreservingPartitioner.
	 */
	private Class<? extends IPartitioner> partitioner = Murmur3Partitioner.class;
	/**
	 * The addresses of hosts designated as contact points in the cluster. A joining node contacts one of the nodes
	 * in the -seeds list to learn the topology of the ring.
	 */
	private ComplexClass<? extends SeedProvider> seedProvider = new ComplexClass<>(SimpleSeedProvider.class,
			Collections.singletonMap("seeds", "localhost"));
	/**
	 * Set to a class that implements the IEndpointSnitch interface. Cassandra uses the snitch to locate nodes and
	 * route requests.
	 */
	private Class<? extends IEndpointSnitch> endpointSnitch = SimpleSnitch.class;
	/**
	 *  Class that implements IRequestScheduler.
	 */
	private Class<? extends IRequestScheduler> requestScheduler = NoScheduler.class;
	/**
	 * Internode authentication backend, implementing IInternodeAuthenticator; used to allow/disallow connections from peer nodes.
	 */
	private Class<? extends IInternodeAuthenticator> internodeAuthenticator = AllowAllInternodeAuthenticator.class;
	/**
	 * Authentication backend, implementing IAuthenticator; used to identify users.
	 */
	private Class<? extends IAuthenticator> authenticator = AllowAllAuthenticator.class;
	/**
	 * Authorization backend, implementing IAuthority; used to limit access/provide permissions.
	 */
	private Class<? extends IAuthorizer> authorizer = AllowAllAuthorizer.class;
	/**
	 * Part of the Authentication & Authorization backend, implementing IRoleManager.
	 */
	private Class<? extends IRoleManager> roleManager = CassandraRoleManager.class;
	/**
	 * Policy for data disk failures.
	 */
	private Config.DiskFailurePolicy diskFailurePolicy = Config.DiskFailurePolicy.ignore;
	/**
	 * Policy for data disk access mode.
	 */
	private Config.DiskAccessMode diskAccessMode = Config.DiskAccessMode.auto;
	/**
	 * Policy for commit disk failures.
	 */
	private Config.CommitFailurePolicy commitFailurePolicy = Config.CommitFailurePolicy.stop;
	/**
	 * Policy for user function timeout policy.
	 */
	private Config.UserFunctionTimeoutPolicy userFunctionTimeoutPolicy = Config.UserFunctionTimeoutPolicy.die;
	/**
	 * Policy for request scheduler id.
	 */
	private Config.RequestSchedulerId requestSchedulerId = Config.RequestSchedulerId.keyspace;
	/**
	 * The strategy for optimizing disk read.
	 */
	private Config.DiskOptimizationStrategy diskOptimizationStrategy = Config.DiskOptimizationStrategy.ssd;
	/**
	 * Compression controls whether traffic between nodes is compressed.
	 */
	private Config.InternodeCompression internodeCompression = Config.InternodeCompression.none;
	/**
	 * Policy for memory table allocation type.
	 */
	private Config.MemtableAllocationType memtableAllocationType = Config.MemtableAllocationType.heap_buffers;
	/**
	 * Compression to apply to the commit log. If omitted, the commit log will be written uncompressed.
	 */
	private ComplexClass<? extends ICompressor> commitLogCompression = new ComplexClass<>();
	/**
	 * Compression to apply to the hint files. If omitted, hints files will be written uncompressed.
	 */
	private ComplexClass<? extends ICompressor> hintsCompression = new ComplexClass<>();
	/**
	 * The back-pressure strategy.
	 */
	private ComplexClass<? extends BackPressureStrategy> backPressureStrategy = new ComplexClass<>();
	/**
	 * Validity period for permissions cache.
	 * Will be disabled automatically for AllowAllAuthorizer.
	 */
	private Duration permissionsValidity = Duration.ofSeconds(2);
	/**
	 * Refresh interval for permissions cache (if enabled).
	 * After this interval, cache entries become eligible for refresh.
	 */
	private Duration permissionsUpdateInterval = Duration.ofSeconds(2);
	/**
	 * Validity period for roles cache
	 * Will be disabled automatically for AllowAllAuthenticator.
	 */
	private Duration rolesValidity = Duration.ofSeconds(2);
	/**
	 * Refresh interval for roles cache (if enabled).
	 * After this interval, cache entries become eligible for refresh.
	 */
	private Duration rolesUpdateInterval = Duration.ofSeconds(2);
	/**
	 * Validity period for credentials cache. This cache is tightly coupled to the provided PasswordAuthenticator
	 * implementation of IAuthenticator. If another IAuthenticator implementation is configured,
	 * this cache will not be automatically used and so the following settings will have no effect.
	 */
	private Duration credentialsValidity = Duration.ofSeconds(2);
	/**
	 * Refresh interval for credentials cache (if enabled).
	 * After this interval, cache entries become eligible for refresh.
	 */
	private Duration credentialsUpdateInterval = Duration.ofSeconds(2);
	/**
	 * The default timeout for other, miscellaneous operations.
	 */
	private Duration requestTimeout = Duration.ofSeconds(10);
	/**
	 * How long the coordinator should wait for read operations to complete.
	 */
	private Duration readRequestTimeout = Duration.ofSeconds(5);
	/**
	 * How long the coordinator should wait for seq or index scans to complete.
	 */
	private Duration rangeRequestTimeout = Duration.ofSeconds(10);
	/**
	 * How long the coordinator should wait for writes to complete.
	 */
	private Duration writeRequestTimeout = Duration.ofSeconds(2);
	/**
	 * How long the coordinator should wait for counter writes to complete.
	 */
	private Duration counterWriteRequestTimeout = Duration.ofSeconds(5);
	/**
	 * How long a coordinator should continue to retry a CAS operation that contends with other proposals for the same row.
	 */
	private Duration casContentionTimeout = Duration.ofSeconds(1);
	/**
	 * How long the coordinator should wait for truncates to complete.
	 */
	private Duration truncateRequestTimeout = Duration.ofMinutes(1);
	/**
	 * How long before a node logs slow queries. Select queries that take longer than this timeout to execute,
	 * will generate an aggregated log message, so that slow queries can be identified.
	 * Set this value to zero to disable slow query logging.
	 */
	private Duration slowQueryLogTimeout = Duration.ofMillis(500);
	/**
	 * Set concurrent reads.
	 */
	private int concurrentReads = 32;
	/**
	 * Set concurrent writes.
	 */
	private int concurrentWrites = 32;
	/**
	 * Set concurrent counter writes.
	 */
	private int concurrentCounterWrites = 32;
	/**
	 * For materialized view writes, as there is a read involved,
	 * so this should be limited by the less of concurrent reads or concurrent writes.
	 */
	private int concurrentMaterializedViewWrites = 32;
	/**
	 * If you choose to specify the interface by name and the interface has an ipv4 and an ipv6 address you can specify
	 * which should be chosen using listenInterfacePreferIpv6. If false the first ipv4 address will be used.
	 * If true the first ipv6 address will be used. Defaults to false preferring ipv4. If there is only one address it will be selected
	 * regardless of ipv4/ipv6.
	 */
	private boolean listenInterfacePreferIpv6 = false;
	/**
	 * Address to broadcast to other Cassandra nodes Leaving this blank will set it to the same value as listenAddress.
	 */
	private String broadcastAddress;
	/**
	 * RPC address to broadcast to drivers and other Cassandra nodes.
	 */
	private String broadcastRpcAddress;
	/**
	 * When using multiple physical network interfaces, set this to true to
	 * listen on broadcastAddress in addition to the listenAddress, allowing nodes to communicate in both interfaces.
	 */
	private boolean listenOnBroadcastAddress = false;
	/**
	 * Type of RPC server.
	 */
	private String rpcType = "sync";
	/**
	 * port for Thrift to listen for clients on.
	 */
	private int rpcPort = 0;
	/**
	 * The address or interface to bind the native transport server to.
	 * Set rpcAddress OR rpcInterface, not both.
	 */
	private String rpcAddress = "localhost";
	/**
	 * Interfaces must correspond to a single address, IP aliasing is not supported.
	 * Set rpcAddress OR rpcInterface, not both.
	 */
	private String rpcInterface;
	/**
	 * If you choose to specify the interface by name and the interface has an ipv4 and an ipv6 address you can
	 * specify  which should be chosen using rpcInterfacePreferIpv6. If false the first ipv4 address will be used.
	 * If true the first ipv6 address will be used. Defaults to false preferring ipv4. If there is only one address
	 * it will be selected regardless of ipv4/ipv6.
	 */
	private boolean rpcInterfacePreferIpv6 = false;
	/**
	 * Enable or disable keepalive on rpc/native connections.
	 */
	private boolean rpcKeepalive = true;
	/**
	 * Whether or not a snapshot is taken of the data before keyspace truncation
	 * or dropping of column families.
	 */
	private boolean autoSnapshot = true;
	/**
	 * Change-data-capture logs.
	 */
	private boolean cdcEnabled = false;
	/**
	 * Client-to-node encryption protects data in flight from client machines to a database cluster
	 * using SSL (Secure Sockets Layer). It establishes a secure channel between the client and the coordinator node.
	 */
	private ClientEncryption clientEncryption = new ClientEncryption();
	/**
	 * Node-to-node encryption protects data transferred between nodes in a cluster, including gossip communications,
	 * using SSL (Secure Sockets Layer).
	 */
	private ServerEncryption serverEncryption = new ServerEncryption();
	/**
	 * The number of milliseconds Cassandra waits before flushing hints from internal buffers to disk.
	 */
	private Duration hintsFlushPeriod = Duration.ofSeconds(10);
	/**
	 * Working directory.
	 */
	private String workingDirectory;
	/**
	 * Startup timeout.
	 */
	private Duration startupTimeout = Duration.ofMinutes(2);
	/**
	 * Additional arguments which should be associated with cassandra process.
	 */
	private final List<String> arguments = new ArrayList<>();

	public List<String> getArguments() {
		return this.arguments;
	}

	public Duration getHintsFlushPeriod() {
		return this.hintsFlushPeriod;
	}

	public void setHintsFlushPeriod(Duration hintsFlushPeriod) {
		this.hintsFlushPeriod = hintsFlushPeriod;
	}

	public String getClusterName() {
		return this.clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getListenAddress() {
		return this.listenAddress;
	}

	public void setListenAddress(String listenAddress) {
		this.listenAddress = listenAddress;
	}

	public String getListenInterface() {
		return this.listenInterface;
	}

	public void setListenInterface(String listenInterface) {
		this.listenInterface = listenInterface;
	}

	public Integer getPortSsl() {
		return this.portSsl;
	}

	public void setPortSsl(Integer portSsl) {
		this.portSsl = portSsl;
	}

	public int getStoragePort() {
		return this.storagePort;
	}

	public void setStoragePort(int storagePort) {
		this.storagePort = storagePort;
	}

	public int getStorageSslPort() {
		return this.storageSslPort;
	}

	public void setStorageSslPort(int storageSslPort) {
		this.storageSslPort = storageSslPort;
	}

	public String getHintsDirectory() {
		return this.hintsDirectory;
	}

	public void setHintsDirectory(String hintsDirectory) {
		this.hintsDirectory = hintsDirectory;
	}

	public String getSavedCachesDirectory() {
		return this.savedCachesDirectory;
	}

	public void setSavedCachesDirectory(String savedCachesDirectory) {
		this.savedCachesDirectory = savedCachesDirectory;
	}

	public String getCommitLogDirectory() {
		return this.commitLogDirectory;
	}

	public void setCommitLogDirectory(String commitLogDirectory) {
		this.commitLogDirectory = commitLogDirectory;
	}

	public String getCdcRawDirectory() {
		return this.cdcRawDirectory;
	}

	public void setCdcRawDirectory(String cdcRawDirectory) {
		this.cdcRawDirectory = cdcRawDirectory;
	}

	public List<String> getDataDirectories() {
		return this.dataDirectories;
	}

	public Config.CommitLogSync getCommitLogSync() {
		return this.commitLogSync;
	}

	public void setCommitLogSync(Config.CommitLogSync commitLogSync) {
		this.commitLogSync = commitLogSync;
	}

	public Duration getCommitLogSyncBatch() {
		return this.commitLogSyncBatch;
	}

	public void setCommitLogSyncBatch(Duration commitLogSyncBatch) {
		this.commitLogSyncBatch = commitLogSyncBatch;
	}

	public Duration getCommitLogSyncPeriod() {
		return this.commitLogSyncPeriod;
	}

	public void setCommitLogSyncPeriod(Duration commitLogSyncPeriod) {
		this.commitLogSyncPeriod = commitLogSyncPeriod;
	}

	public Class<? extends IPartitioner> getPartitioner() {
		return this.partitioner;
	}

	public void setPartitioner(Class<? extends IPartitioner> partitioner) {
		this.partitioner = partitioner;
	}

	public ComplexClass<? extends SeedProvider> getSeedProvider() {
		return this.seedProvider;
	}

	public void setSeedProvider(ComplexClass<? extends SeedProvider> seedProvider) {
		this.seedProvider = seedProvider;
	}

	public Class<? extends IEndpointSnitch> getEndpointSnitch() {
		return this.endpointSnitch;
	}

	public void setEndpointSnitch(Class<? extends IEndpointSnitch> endpointSnitch) {
		this.endpointSnitch = endpointSnitch;
	}

	public Class<? extends IRequestScheduler> getRequestScheduler() {
		return this.requestScheduler;
	}

	public void setRequestScheduler(Class<? extends IRequestScheduler> requestScheduler) {
		this.requestScheduler = requestScheduler;
	}

	public Class<? extends IInternodeAuthenticator> getInternodeAuthenticator() {
		return this.internodeAuthenticator;
	}

	public void setInternodeAuthenticator(Class<? extends IInternodeAuthenticator> internodeAuthenticator) {
		this.internodeAuthenticator = internodeAuthenticator;
	}

	public Class<? extends IAuthenticator> getAuthenticator() {
		return this.authenticator;
	}

	public void setAuthenticator(Class<? extends IAuthenticator> authenticator) {
		this.authenticator = authenticator;
	}

	public Class<? extends IAuthorizer> getAuthorizer() {
		return this.authorizer;
	}

	public void setAuthorizer(Class<? extends IAuthorizer> authorizer) {
		this.authorizer = authorizer;
	}

	public Class<? extends IRoleManager> getRoleManager() {
		return this.roleManager;
	}

	public void setRoleManager(Class<? extends IRoleManager> roleManager) {
		this.roleManager = roleManager;
	}

	public Config.DiskFailurePolicy getDiskFailurePolicy() {
		return this.diskFailurePolicy;
	}

	public void setDiskFailurePolicy(Config.DiskFailurePolicy diskFailurePolicy) {
		this.diskFailurePolicy = diskFailurePolicy;
	}

	public Config.DiskAccessMode getDiskAccessMode() {
		return this.diskAccessMode;
	}

	public void setDiskAccessMode(Config.DiskAccessMode diskAccessMode) {
		this.diskAccessMode = diskAccessMode;
	}

	public Config.CommitFailurePolicy getCommitFailurePolicy() {
		return this.commitFailurePolicy;
	}

	public void setCommitFailurePolicy(Config.CommitFailurePolicy commitFailurePolicy) {
		this.commitFailurePolicy = commitFailurePolicy;
	}

	public Config.UserFunctionTimeoutPolicy getUserFunctionTimeoutPolicy() {
		return this.userFunctionTimeoutPolicy;
	}

	public void setUserFunctionTimeoutPolicy(Config.UserFunctionTimeoutPolicy userFunctionTimeoutPolicy) {
		this.userFunctionTimeoutPolicy = userFunctionTimeoutPolicy;
	}

	public Config.RequestSchedulerId getRequestSchedulerId() {
		return this.requestSchedulerId;
	}

	public void setRequestSchedulerId(Config.RequestSchedulerId requestSchedulerId) {
		this.requestSchedulerId = requestSchedulerId;
	}

	public Config.DiskOptimizationStrategy getDiskOptimizationStrategy() {
		return this.diskOptimizationStrategy;
	}

	public void setDiskOptimizationStrategy(Config.DiskOptimizationStrategy diskOptimizationStrategy) {
		this.diskOptimizationStrategy = diskOptimizationStrategy;
	}

	public Config.InternodeCompression getInternodeCompression() {
		return this.internodeCompression;
	}

	public void setInternodeCompression(Config.InternodeCompression internodeCompression) {
		this.internodeCompression = internodeCompression;
	}

	public Config.MemtableAllocationType getMemtableAllocationType() {
		return this.memtableAllocationType;
	}

	public void setMemtableAllocationType(Config.MemtableAllocationType memtableAllocationType) {
		this.memtableAllocationType = memtableAllocationType;
	}

	public String getWorkingDirectory() {
		return this.workingDirectory;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	public ComplexClass<? extends ICompressor> getCommitLogCompression() {
		return this.commitLogCompression;
	}

	public void setCommitLogCompression(ComplexClass<? extends ICompressor> commitLogCompression) {
		this.commitLogCompression = commitLogCompression;
	}

	public ComplexClass<? extends ICompressor> getHintsCompression() {
		return this.hintsCompression;
	}

	public void setHintsCompression(ComplexClass<? extends ICompressor> hintsCompression) {
		this.hintsCompression = hintsCompression;
	}

	public ComplexClass<? extends BackPressureStrategy> getBackPressureStrategy() {
		return this.backPressureStrategy;
	}

	public void setBackPressureStrategy(ComplexClass<? extends BackPressureStrategy> backPressureStrategy) {
		this.backPressureStrategy = backPressureStrategy;
	}

	public Duration getStartupTimeout() {
		return this.startupTimeout;
	}

	public void setStartupTimeout(Duration startupTimeout) {
		this.startupTimeout = startupTimeout;
	}

	public Duration getPermissionsValidity() {
		return this.permissionsValidity;
	}

	public void setPermissionsValidity(Duration permissionsValidity) {
		this.permissionsValidity = permissionsValidity;
	}

	public Duration getPermissionsUpdateInterval() {
		return this.permissionsUpdateInterval;
	}

	public void setPermissionsUpdateInterval(Duration permissionsUpdateInterval) {
		this.permissionsUpdateInterval = permissionsUpdateInterval;
	}

	public Duration getRolesValidity() {
		return this.rolesValidity;
	}

	public void setRolesValidity(Duration rolesValidity) {
		this.rolesValidity = rolesValidity;
	}

	public Duration getRolesUpdateInterval() {
		return this.rolesUpdateInterval;
	}

	public void setRolesUpdateInterval(Duration rolesUpdateInterval) {
		this.rolesUpdateInterval = rolesUpdateInterval;
	}

	public Duration getCredentialsValidity() {
		return this.credentialsValidity;
	}

	public void setCredentialsValidity(Duration credentialsValidity) {
		this.credentialsValidity = credentialsValidity;
	}

	public Duration getCredentialsUpdateInterval() {
		return this.credentialsUpdateInterval;
	}

	public void setCredentialsUpdateInterval(Duration credentialsUpdateInterval) {
		this.credentialsUpdateInterval = credentialsUpdateInterval;
	}

	public Duration getRequestTimeout() {
		return this.requestTimeout;
	}

	public void setRequestTimeout(Duration requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public Duration getReadRequestTimeout() {
		return this.readRequestTimeout;
	}

	public void setReadRequestTimeout(Duration readRequestTimeout) {
		this.readRequestTimeout = readRequestTimeout;
	}

	public Duration getRangeRequestTimeout() {
		return this.rangeRequestTimeout;
	}

	public void setRangeRequestTimeout(Duration rangeRequestTimeout) {
		this.rangeRequestTimeout = rangeRequestTimeout;
	}

	public Duration getWriteRequestTimeout() {
		return this.writeRequestTimeout;
	}

	public void setWriteRequestTimeout(Duration writeRequestTimeout) {
		this.writeRequestTimeout = writeRequestTimeout;
	}

	public Duration getCounterWriteRequestTimeout() {
		return this.counterWriteRequestTimeout;
	}

	public void setCounterWriteRequestTimeout(Duration counterWriteRequestTimeout) {
		this.counterWriteRequestTimeout = counterWriteRequestTimeout;
	}

	public Duration getCasContentionTimeout() {
		return this.casContentionTimeout;
	}

	public void setCasContentionTimeout(Duration casContentionTimeout) {
		this.casContentionTimeout = casContentionTimeout;
	}

	public Duration getTruncateRequestTimeout() {
		return this.truncateRequestTimeout;
	}

	public void setTruncateRequestTimeout(Duration truncateRequestTimeout) {
		this.truncateRequestTimeout = truncateRequestTimeout;
	}

	public Duration getSlowQueryLogTimeout() {
		return this.slowQueryLogTimeout;
	}

	public void setSlowQueryLogTimeout(Duration slowQueryLogTimeout) {
		this.slowQueryLogTimeout = slowQueryLogTimeout;
	}

	public int getConcurrentReads() {
		return this.concurrentReads;
	}

	public void setConcurrentReads(int concurrentReads) {
		this.concurrentReads = concurrentReads;
	}

	public int getConcurrentWrites() {
		return this.concurrentWrites;
	}

	public void setConcurrentWrites(int concurrentWrites) {
		this.concurrentWrites = concurrentWrites;
	}

	public int getConcurrentCounterWrites() {
		return this.concurrentCounterWrites;
	}

	public void setConcurrentCounterWrites(int concurrentCounterWrites) {
		this.concurrentCounterWrites = concurrentCounterWrites;
	}

	public int getConcurrentMaterializedViewWrites() {
		return this.concurrentMaterializedViewWrites;
	}

	public void setConcurrentMaterializedViewWrites(int concurrentMaterializedViewWrites) {
		this.concurrentMaterializedViewWrites = concurrentMaterializedViewWrites;
	}

	public boolean isListenInterfacePreferIpv6() {
		return this.listenInterfacePreferIpv6;
	}

	public void setListenInterfacePreferIpv6(boolean listenInterfacePreferIpv6) {
		this.listenInterfacePreferIpv6 = listenInterfacePreferIpv6;
	}

	public String getBroadcastAddress() {
		return this.broadcastAddress;
	}

	public void setBroadcastAddress(String broadcastAddress) {
		this.broadcastAddress = broadcastAddress;
	}

	public String getBroadcastRpcAddress() {
		return this.broadcastRpcAddress;
	}

	public void setBroadcastRpcAddress(String broadcastRpcAddress) {
		this.broadcastRpcAddress = broadcastRpcAddress;
	}

	public boolean isListenOnBroadcastAddress() {
		return this.listenOnBroadcastAddress;
	}

	public void setListenOnBroadcastAddress(boolean listenOnBroadcastAddress) {
		this.listenOnBroadcastAddress = listenOnBroadcastAddress;
	}

	public String getRpcType() {
		return this.rpcType;
	}

	public void setRpcType(String rpcType) {
		this.rpcType = rpcType;
	}

	public int getRpcPort() {
		return this.rpcPort;
	}

	public void setRpcPort(int rpcPort) {
		this.rpcPort = rpcPort;
	}

	public String getRpcAddress() {
		return this.rpcAddress;
	}

	public void setRpcAddress(String rpcAddress) {
		this.rpcAddress = rpcAddress;
	}

	public String getRpcInterface() {
		return this.rpcInterface;
	}

	public void setRpcInterface(String rpcInterface) {
		this.rpcInterface = rpcInterface;
	}

	public boolean isRpcInterfacePreferIpv6() {
		return this.rpcInterfacePreferIpv6;
	}

	public void setRpcInterfacePreferIpv6(boolean rpcInterfacePreferIpv6) {
		this.rpcInterfacePreferIpv6 = rpcInterfacePreferIpv6;
	}

	public boolean isRpcKeepalive() {
		return this.rpcKeepalive;
	}

	public void setRpcKeepalive(boolean rpcKeepalive) {
		this.rpcKeepalive = rpcKeepalive;
	}

	public boolean isAutoSnapshot() {
		return this.autoSnapshot;
	}

	public void setAutoSnapshot(boolean autoSnapshot) {
		this.autoSnapshot = autoSnapshot;
	}

	public boolean isCdcEnabled() {
		return this.cdcEnabled;
	}

	public void setCdcEnabled(boolean cdcEnabled) {
		this.cdcEnabled = cdcEnabled;
	}

	public ClientEncryption getClientEncryption() {
		return this.clientEncryption;
	}

	public void setClientEncryption(ClientEncryption clientEncryption) {
		this.clientEncryption = clientEncryption;
	}

	public ServerEncryption getServerEncryption() {
		return this.serverEncryption;
	}

	public void setServerEncryption(ServerEncryption serverEncryption) {
		this.serverEncryption = serverEncryption;
	}

	/**
	 * Simple wrapper for complex implementation classes.
	 *
	 * @param <T> implementation class.
	 */
	public static class ComplexClass<T> {
		/**
		 * Parameters.
		 */
		private final Map<String, String> parameters = new LinkedHashMap<>();
		/**
		 * Target class.
		 */
		private Class<T> targetClass;

		public ComplexClass() {
		}

		private ComplexClass(Class<T> targetClass, Map<String, String> parameters) {
			this.targetClass = targetClass;
			getParameters().putAll(parameters);
		}

		public Class<T> getTargetClass() {
			return this.targetClass;
		}

		public void setTargetClass(Class<T> targetClass) {
			this.targetClass = targetClass;
		}

		public Map<String, String> getParameters() {
			return this.parameters;
		}
	}

	/**
	 * Client-to-node encryption protects data in flight from client machines to a database cluster
	 * using SSL (Secure Sockets Layer). It establishes a secure channel between the client and the coordinator node.
	 */
	public static class ClientEncryption {
		/**
		 * The location of a Java keystore (JKS) suitable for use with Java Secure Socket Extension (JSSE),
		 * which is the Java version of the Secure Sockets Layer (SSL), and Transport Layer Security (TLS) protocols.
		 * The keystore contains the private key used to encrypt outgoing messages.
		 */
		private String keystore;
		/**
		 * Password for the keystore.
		 */
		private String keystorePassword;
		/**
		 * Location of the truststore containing the trusted certificate for authenticating remote servers.
		 */
		private String truststore;
		/**
		 * Password for the truststore.
		 */
		private String truststorePassword;
		/**
		 * Set the protocol.
		 */
		private String protocol;
		/**
		 * Set the algorithm.
		 */
		private String algorithm;
		/**
		 * Set the store type.
		 */
		private String storeType;
		/**
		 * Enables or disables certificate authentication.
		 */
		private boolean requireClientAuth = false;
		/**
		 * Enables or disables host name verification.
		 */
		private boolean requireEndpointVerification = false;
		/**
		 * To enable, set to true.
		 */
		private boolean enabled = false;
		/**
		 * If enabled and optional is set to true encrypted and unencrypted connections are handled.
		 */
		private boolean optional = false;
		/**
		 * Add cipher suites.
		 */
		private final List<String> cipherSuites = new ArrayList<>();

		public String getKeystore() {
			return this.keystore;
		}

		public void setKeystore(String keystore) {
			this.keystore = keystore;
		}

		public String getKeystorePassword() {
			return this.keystorePassword;
		}

		public void setKeystorePassword(String keystorePassword) {
			this.keystorePassword = keystorePassword;
		}

		public String getTruststore() {
			return this.truststore;
		}

		public void setTruststore(String truststore) {
			this.truststore = truststore;
		}

		public String getTruststorePassword() {
			return this.truststorePassword;
		}

		public void setTruststorePassword(String truststorePassword) {
			this.truststorePassword = truststorePassword;
		}

		public List<String> getCipherSuites() {
			return this.cipherSuites;
		}

		public String getProtocol() {
			return this.protocol;
		}

		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

		public String getAlgorithm() {
			return this.algorithm;
		}

		public void setAlgorithm(String algorithm) {
			this.algorithm = algorithm;
		}

		public String getStoreType() {
			return this.storeType;
		}

		public void setStoreType(String storeType) {
			this.storeType = storeType;
		}

		public boolean isRequireClientAuth() {
			return this.requireClientAuth;
		}

		public void setRequireClientAuth(boolean requireClientAuth) {
			this.requireClientAuth = requireClientAuth;
		}

		public boolean isRequireEndpointVerification() {
			return this.requireEndpointVerification;
		}

		public void setRequireEndpointVerification(boolean requireEndpointVerification) {
			this.requireEndpointVerification = requireEndpointVerification;
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isOptional() {
			return this.optional;
		}

		public void setOptional(boolean optional) {
			this.optional = optional;
		}
	}

	/**
	 * Node-to-node encryption protects data transferred between nodes in a cluster, including gossip communications,
	 * using SSL (Secure Sockets Layer).
	 */
	public static class ServerEncryption {
		/**
		 * The location of a Java keystore (JKS) suitable for use with Java Secure Socket Extension (JSSE),
		 * which is the Java version of the Secure Sockets Layer (SSL), and Transport Layer Security (TLS) protocols.
		 * The keystore contains the private key used to encrypt outgoing messages.
		 */
		private String keystore;
		/**
		 * Password for the keystore.
		 */
		private String keystorePassword;
		/**
		 * Location of the truststore containing the trusted certificate for authenticating remote servers.
		 */
		private String truststore;
		/**
		 * Password for the truststore.
		 */
		private String truststorePassword;
		/**
		 * Set the protocol.
		 */
		private String protocol;
		/**
		 * Set the algorithm.
		 */
		private String algorithm;
		/**
		 * Set the store type.
		 */
		private String storeType;
		/**
		 * Enables or disables certificate authentication.
		 */
		private boolean requireClientAuth = false;
		/**
		 * Enables or disables host name verification.
		 */
		private boolean requireEndpointVerification = false;
		/**
		 * Policy for internode encryption.
		 */
		private InternodeEncryption internodeEncryption = InternodeEncryption.none;
		/**
		 * Added cipher suites.
		 */
		private final List<String> cipherSuites = new ArrayList<>();

		public String getKeystore() {
			return this.keystore;
		}

		public void setKeystore(String keystore) {
			this.keystore = keystore;
		}

		public String getKeystorePassword() {
			return this.keystorePassword;
		}

		public void setKeystorePassword(String keystorePassword) {
			this.keystorePassword = keystorePassword;
		}

		public String getTruststore() {
			return this.truststore;
		}

		public void setTruststore(String truststore) {
			this.truststore = truststore;
		}

		public String getTruststorePassword() {
			return this.truststorePassword;
		}

		public void setTruststorePassword(String truststorePassword) {
			this.truststorePassword = truststorePassword;
		}

		public List<String> getCipherSuites() {
			return this.cipherSuites;
		}

		public String getProtocol() {
			return this.protocol;
		}

		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

		public String getAlgorithm() {
			return this.algorithm;
		}

		public void setAlgorithm(String algorithm) {
			this.algorithm = algorithm;
		}

		public String getStoreType() {
			return this.storeType;
		}

		public void setStoreType(String storeType) {
			this.storeType = storeType;
		}

		public boolean isRequireClientAuth() {
			return this.requireClientAuth;
		}

		public void setRequireClientAuth(boolean requireClientAuth) {
			this.requireClientAuth = requireClientAuth;
		}

		public boolean isRequireEndpointVerification() {
			return this.requireEndpointVerification;
		}

		public void setRequireEndpointVerification(boolean requireEndpointVerification) {
			this.requireEndpointVerification = requireEndpointVerification;
		}

		public InternodeEncryption getInternodeEncryption() {
			return this.internodeEncryption;
		}

		public void setInternodeEncryption(InternodeEncryption internodeEncryption) {
			this.internodeEncryption = internodeEncryption;
		}
	}

}

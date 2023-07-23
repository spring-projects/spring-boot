/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.pulsar;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.common.schema.SchemaType;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.pulsar.core.PulsarAdminBuilderCustomizer;
import org.springframework.pulsar.core.ReaderBuilderCustomizer;
import org.springframework.pulsar.listener.AckMode;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * Configuration properties for Spring for Apache Pulsar.
 * <p>
 * Users should refer to Pulsar documentation for complete descriptions of these
 * properties.
 *
 * @author Soby Chacko
 * @author Alexander Preuß
 * @author Christophe Bornet
 * @author Chris Bono
 * @author Kevin Lu
 * @since 3.2.0
 */
@ConfigurationProperties(prefix = "spring.pulsar")
public class PulsarProperties {

	@NestedConfigurationProperty
	private final ConsumerConfigProperties consumer = new ConsumerConfigProperties();

	private final Client client = new Client();

	private final Function function = new Function();

	private final Listener listener = new Listener();

	@NestedConfigurationProperty
	private final ProducerConfigProperties producer = new ProducerConfigProperties();

	private final Template template = new Template();

	private final Admin admin = new Admin();

	private final Reader reader = new Reader();

	private final Defaults defaults = new Defaults();

	public ConsumerConfigProperties getConsumer() {
		return this.consumer;
	}

	public Client getClient() {
		return this.client;
	}

	public Listener getListener() {
		return this.listener;
	}

	public Function getFunction() {
		return this.function;
	}

	public ProducerConfigProperties getProducer() {
		return this.producer;
	}

	public Template getTemplate() {
		return this.template;
	}

	public Admin getAdministration() {
		return this.admin;
	}

	public Reader getReader() {
		return this.reader;
	}

	public Defaults getDefaults() {
		return this.defaults;
	}

	public static class Template {

		/**
		 * Whether to record observations for send operations when the Observations API is
		 * available.
		 */
		private Boolean observationsEnabled = true;

		public Boolean isObservationsEnabled() {
			return this.observationsEnabled;
		}

		public void setObservationsEnabled(Boolean observationsEnabled) {
			this.observationsEnabled = observationsEnabled;
		}

	}

	public static class Cache {

		/** Time period to expire unused entries in the cache. */
		private Duration expireAfterAccess = Duration.ofMinutes(1);

		/** Maximum size of cache (entries). */
		private Long maximumSize = 1000L;

		/** Initial size of cache. */
		private Integer initialCapacity = 50;

		public Duration getExpireAfterAccess() {
			return this.expireAfterAccess;
		}

		public void setExpireAfterAccess(Duration expireAfterAccess) {
			this.expireAfterAccess = expireAfterAccess;
		}

		public Long getMaximumSize() {
			return this.maximumSize;
		}

		public void setMaximumSize(Long maximumSize) {
			this.maximumSize = maximumSize;
		}

		public Integer getInitialCapacity() {
			return this.initialCapacity;
		}

		public void setInitialCapacity(Integer initialCapacity) {
			this.initialCapacity = initialCapacity;
		}

	}

	public static class Client {

		/**
		 * Pulsar service URL in the format
		 * '(pulsar|pulsar+ssl)://&lt;host&gt;:&lt;port&gt;'.
		 */
		private String serviceUrl = "pulsar://localhost:6650";

		/**
		 * Listener name for lookup. Clients can use listenerName to choose one of the
		 * listeners as the service URL to create a connection to the broker. To use this,
		 * "advertisedListeners" must be enabled on the broker.
		 */
		private String listenerName;

		/**
		 * Fully qualified class name of the authentication plugin.
		 */
		private String authPluginClassName;

		/**
		 * Authentication parameter(s) as a JSON encoded string.
		 */
		private String authParams;

		/**
		 * Authentication parameter(s) as a map of parameter names to parameter values.
		 */
		private Map<String, String> authentication;

		/**
		 * Client operation timeout.
		 */
		private Duration operationTimeout = Duration.ofSeconds(30);

		/**
		 * Client lookup timeout.
		 */
		private Duration lookupTimeout = Duration.ofMillis(-1);

		/**
		 * Number of threads to be used for handling connections to brokers.
		 */
		private Integer numIoThreads = 1;

		/**
		 * Number of threads to be used for message listeners. The listener thread pool is
		 * shared across all the consumers and readers that are using a "listener" model
		 * to get messages. For a given consumer, the listener will always be invoked from
		 * the same thread, to ensure ordering.
		 */
		private Integer numListenerThreads = 1;

		/**
		 * Maximum number of connections that the client will open to a single broker.
		 */
		private Integer numConnectionsPerBroker = 1;

		/**
		 * Whether to use TCP no-delay flag on the connection, to disable Nagle algorithm.
		 */
		private Boolean useTcpNoDelay = true;

		/**
		 * Whether to use TLS encryption on the connection.
		 */
		private Boolean useTls = false;

		/**
		 * Whether the hostname is validated when the proxy creates a TLS connection with
		 * brokers.
		 */
		private Boolean tlsHostnameVerificationEnable = false;

		/**
		 * Path to the trusted TLS certificate file.
		 */
		private String tlsTrustCertsFilePath;

		/**
		 * Path to the TLS certificate file.
		 */
		private String tlsCertificateFilePath;

		/**
		 * Path to the TLS private key file.
		 */
		private String tlsKeyFilePath;

		/**
		 * Whether the client accepts untrusted TLS certificates from the broker.
		 */
		private Boolean tlsAllowInsecureConnection = false;

		/**
		 * Enable KeyStore instead of PEM type configuration if TLS is enabled.
		 */
		private Boolean useKeyStoreTls = false;

		/**
		 * Name of the security provider used for SSL connections.
		 */
		private String sslProvider;

		/**
		 * File format of the trust store file.
		 */
		private String tlsTrustStoreType;

		/**
		 * Location of the trust store file.
		 */
		private String tlsTrustStorePath;

		/**
		 * Store password for the key store file.
		 */
		private String tlsTrustStorePassword;

		/**
		 * Comma-separated list of cipher suites. This is a named combination of
		 * authentication, encryption, MAC and key exchange algorithm used to negotiate
		 * the security settings for a network connection using TLS or SSL network
		 * protocol. By default, all the available cipher suites are supported.
		 */
		private Set<String> tlsCiphers;

		/**
		 * Comma-separated list of SSL protocols used to generate the SSLContext. Allowed
		 * values in recent JVMs are TLS, TLSv1.3, TLSv1.2 and TLSv1.1.
		 */
		private Set<String> tlsProtocols;

		/**
		 * Interval between each stat info.
		 */
		private Duration statsInterval = Duration.ofSeconds(60);

		/**
		 * Number of concurrent lookup-requests allowed to send on each broker-connection
		 * to prevent overload on broker.
		 */
		private Integer maxConcurrentLookupRequest = 5000;

		/**
		 * Number of max lookup-requests allowed on each broker-connection to prevent
		 * overload on broker.
		 */
		private Integer maxLookupRequest = 50000;

		/**
		 * Maximum number of times a lookup-request to a broker will be redirected.
		 */
		private Integer maxLookupRedirects = 20;

		/**
		 * Maximum number of broker-rejected requests in a certain timeframe, after which
		 * the current connection is closed and a new connection is created by the client.
		 */
		private Integer maxNumberOfRejectedRequestPerConnection = 50;

		/**
		 * Keep alive interval for broker-client connection.
		 */
		private Duration keepAliveInterval = Duration.ofSeconds(30);

		/**
		 * Duration to wait for a connection to a broker to be established.
		 */
		private Duration connectionTimeout = Duration.ofSeconds(10);

		/**
		 * Maximum duration for completing a request.
		 */
		private Duration requestTimeout = Duration.ofMinutes(1);

		/**
		 * Initial backoff interval.
		 */
		private Duration initialBackoffInterval = Duration.ofMillis(100);

		/**
		 * Maximum backoff interval.
		 */
		private Duration maxBackoffInterval = Duration.ofSeconds(30);

		/**
		 * Enables spin-waiting on executors and IO threads in order to reduce latency
		 * during context switches.
		 */
		private Boolean enableBusyWait = false;

		/**
		 * Limit of direct memory that will be allocated by the client.
		 */
		private DataSize memoryLimit = DataSize.ofMegabytes(64);

		/**
		 * URL of proxy service. proxyServiceUrl and proxyProtocol must be mutually
		 * inclusive.
		 */
		private String proxyServiceUrl;

		/**
		 * Enables transactions. To use this, start the transactionCoordinatorClient with
		 * the pulsar client.
		 */
		private Boolean enableTransaction = false;

		/**
		 * DNS lookup bind address.
		 */
		private String dnsLookupBindAddress;

		/**
		 * DNS lookup bind port.
		 */
		private Integer dnsLookupBindPort = 0;

		/**
		 * SOCKS5 proxy address.
		 */
		private String socks5ProxyAddress;

		/**
		 * SOCKS5 proxy username.
		 */
		private String socks5ProxyUsername;

		/**
		 * SOCKS5 proxy password.
		 */
		private String socks5ProxyPassword;

		public String getServiceUrl() {
			return this.serviceUrl;
		}

		public void setServiceUrl(String serviceUrl) {
			this.serviceUrl = serviceUrl;
		}

		public String getListenerName() {
			return this.listenerName;
		}

		public void setListenerName(String listenerName) {
			this.listenerName = listenerName;
		}

		public String getAuthPluginClassName() {
			return this.authPluginClassName;
		}

		public void setAuthPluginClassName(String authPluginClassName) {
			this.authPluginClassName = authPluginClassName;
		}

		public String getAuthParams() {
			return this.authParams;
		}

		public void setAuthParams(String authParams) {
			this.authParams = authParams;
		}

		public Map<String, String> getAuthentication() {
			return this.authentication;
		}

		public void setAuthentication(Map<String, String> authentication) {
			this.authentication = authentication;
		}

		public Duration getOperationTimeout() {
			return this.operationTimeout;
		}

		public void setOperationTimeout(Duration operationTimeout) {
			this.operationTimeout = operationTimeout;
		}

		public Duration getLookupTimeout() {
			return this.lookupTimeout;
		}

		public void setLookupTimeout(Duration lookupTimeout) {
			this.lookupTimeout = lookupTimeout;
		}

		public Integer getNumIoThreads() {
			return this.numIoThreads;
		}

		public void setNumIoThreads(Integer numIoThreads) {
			this.numIoThreads = numIoThreads;
		}

		public Integer getNumListenerThreads() {
			return this.numListenerThreads;
		}

		public void setNumListenerThreads(Integer numListenerThreads) {
			this.numListenerThreads = numListenerThreads;
		}

		public Integer getNumConnectionsPerBroker() {
			return this.numConnectionsPerBroker;
		}

		public void setNumConnectionsPerBroker(Integer numConnectionsPerBroker) {
			this.numConnectionsPerBroker = numConnectionsPerBroker;
		}

		public Boolean getUseTcpNoDelay() {
			return this.useTcpNoDelay;
		}

		public void setUseTcpNoDelay(Boolean useTcpNoDelay) {
			this.useTcpNoDelay = useTcpNoDelay;
		}

		public Boolean getUseTls() {
			return this.useTls;
		}

		public void setUseTls(Boolean useTls) {
			this.useTls = useTls;
		}

		public Boolean getTlsHostnameVerificationEnable() {
			return this.tlsHostnameVerificationEnable;
		}

		public void setTlsHostnameVerificationEnable(Boolean tlsHostnameVerificationEnable) {
			this.tlsHostnameVerificationEnable = tlsHostnameVerificationEnable;
		}

		public String getTlsTrustCertsFilePath() {
			return this.tlsTrustCertsFilePath;
		}

		public void setTlsTrustCertsFilePath(String tlsTrustCertsFilePath) {
			this.tlsTrustCertsFilePath = tlsTrustCertsFilePath;
		}

		public String getTlsCertificateFilePath() {
			return this.tlsCertificateFilePath;
		}

		public void setTlsCertificateFilePath(String tlsCertificateFilePath) {
			this.tlsCertificateFilePath = tlsCertificateFilePath;
		}

		public String getTlsKeyFilePath() {
			return this.tlsKeyFilePath;
		}

		public void setTlsKeyFilePath(String tlsKeyFilePath) {
			this.tlsKeyFilePath = tlsKeyFilePath;
		}

		public Boolean getTlsAllowInsecureConnection() {
			return this.tlsAllowInsecureConnection;
		}

		public void setTlsAllowInsecureConnection(Boolean tlsAllowInsecureConnection) {
			this.tlsAllowInsecureConnection = tlsAllowInsecureConnection;
		}

		public Boolean getUseKeyStoreTls() {
			return this.useKeyStoreTls;
		}

		public void setUseKeyStoreTls(Boolean useKeyStoreTls) {
			this.useKeyStoreTls = useKeyStoreTls;
		}

		public String getSslProvider() {
			return this.sslProvider;
		}

		public void setSslProvider(String sslProvider) {
			this.sslProvider = sslProvider;
		}

		public String getTlsTrustStoreType() {
			return this.tlsTrustStoreType;
		}

		public void setTlsTrustStoreType(String tlsTrustStoreType) {
			this.tlsTrustStoreType = tlsTrustStoreType;
		}

		public String getTlsTrustStorePath() {
			return this.tlsTrustStorePath;
		}

		public void setTlsTrustStorePath(String tlsTrustStorePath) {
			this.tlsTrustStorePath = tlsTrustStorePath;
		}

		public String getTlsTrustStorePassword() {
			return this.tlsTrustStorePassword;
		}

		public void setTlsTrustStorePassword(String tlsTrustStorePassword) {
			this.tlsTrustStorePassword = tlsTrustStorePassword;
		}

		public Set<String> getTlsCiphers() {
			return this.tlsCiphers;
		}

		public void setTlsCiphers(Set<String> tlsCiphers) {
			this.tlsCiphers = tlsCiphers;
		}

		public Set<String> getTlsProtocols() {
			return this.tlsProtocols;
		}

		public void setTlsProtocols(Set<String> tlsProtocols) {
			this.tlsProtocols = tlsProtocols;
		}

		public Duration getStatsInterval() {
			return this.statsInterval;
		}

		public void setStatsInterval(Duration statsInterval) {
			this.statsInterval = statsInterval;
		}

		public Integer getMaxConcurrentLookupRequest() {
			return this.maxConcurrentLookupRequest;
		}

		public void setMaxConcurrentLookupRequest(Integer maxConcurrentLookupRequest) {
			this.maxConcurrentLookupRequest = maxConcurrentLookupRequest;
		}

		public Integer getMaxLookupRequest() {
			return this.maxLookupRequest;
		}

		public void setMaxLookupRequest(Integer maxLookupRequest) {
			this.maxLookupRequest = maxLookupRequest;
		}

		public Integer getMaxLookupRedirects() {
			return this.maxLookupRedirects;
		}

		public void setMaxLookupRedirects(Integer maxLookupRedirects) {
			this.maxLookupRedirects = maxLookupRedirects;
		}

		public Integer getMaxNumberOfRejectedRequestPerConnection() {
			return this.maxNumberOfRejectedRequestPerConnection;
		}

		public void setMaxNumberOfRejectedRequestPerConnection(Integer maxNumberOfRejectedRequestPerConnection) {
			this.maxNumberOfRejectedRequestPerConnection = maxNumberOfRejectedRequestPerConnection;
		}

		public Duration getKeepAliveInterval() {
			return this.keepAliveInterval;
		}

		public void setKeepAliveInterval(Duration keepAliveInterval) {
			this.keepAliveInterval = keepAliveInterval;
		}

		public Duration getConnectionTimeout() {
			return this.connectionTimeout;
		}

		public void setConnectionTimeout(Duration connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		public Duration getRequestTimeout() {
			return this.requestTimeout;
		}

		public void setRequestTimeout(Duration requestTimeout) {
			this.requestTimeout = requestTimeout;
		}

		public Duration getInitialBackoffInterval() {
			return this.initialBackoffInterval;
		}

		public void setInitialBackoffInterval(Duration initialBackoffInterval) {
			this.initialBackoffInterval = initialBackoffInterval;
		}

		public Duration getMaxBackoffInterval() {
			return this.maxBackoffInterval;
		}

		public void setMaxBackoffInterval(Duration maxBackoffInterval) {
			this.maxBackoffInterval = maxBackoffInterval;
		}

		public Boolean getEnableBusyWait() {
			return this.enableBusyWait;
		}

		public void setEnableBusyWait(Boolean enableBusyWait) {
			this.enableBusyWait = enableBusyWait;
		}

		public DataSize getMemoryLimit() {
			return this.memoryLimit;
		}

		public void setMemoryLimit(DataSize memoryLimit) {
			this.memoryLimit = memoryLimit;
		}

		public String getProxyServiceUrl() {
			return this.proxyServiceUrl;
		}

		public void setProxyServiceUrl(String proxyServiceUrl) {
			this.proxyServiceUrl = proxyServiceUrl;
		}

		public Boolean getEnableTransaction() {
			return this.enableTransaction;
		}

		public void setEnableTransaction(Boolean enableTransaction) {
			this.enableTransaction = enableTransaction;
		}

		public String getDnsLookupBindAddress() {
			return this.dnsLookupBindAddress;
		}

		public void setDnsLookupBindAddress(String dnsLookupBindAddress) {
			this.dnsLookupBindAddress = dnsLookupBindAddress;
		}

		public Integer getDnsLookupBindPort() {
			return this.dnsLookupBindPort;
		}

		public void setDnsLookupBindPort(Integer dnsLookupBindPort) {
			this.dnsLookupBindPort = dnsLookupBindPort;
		}

		public String getSocks5ProxyAddress() {
			return this.socks5ProxyAddress;
		}

		public void setSocks5ProxyAddress(String socks5ProxyAddress) {
			this.socks5ProxyAddress = socks5ProxyAddress;
		}

		public String getSocks5ProxyUsername() {
			return this.socks5ProxyUsername;
		}

		public void setSocks5ProxyUsername(String socks5ProxyUsername) {
			this.socks5ProxyUsername = socks5ProxyUsername;
		}

		public String getSocks5ProxyPassword() {
			return this.socks5ProxyPassword;
		}

		public void setSocks5ProxyPassword(String socks5ProxyPassword) {
			this.socks5ProxyPassword = socks5ProxyPassword;
		}

	}

	public static class Function {

		/**
		 * Whether to stop processing further function creates/updates when a failure
		 * occurs.
		 */
		private Boolean failFast = Boolean.TRUE;

		/**
		 * Whether to throw an exception if any failure is encountered during server
		 * startup while creating/updating functions.
		 */
		private Boolean propagateFailures = Boolean.TRUE;

		/**
		 * Whether to throw an exception if any failure is encountered during server
		 * shutdown while enforcing stop policy on functions.
		 */
		private Boolean propagateStopFailures = Boolean.FALSE;

		public Boolean getFailFast() {
			return this.failFast;
		}

		public void setFailFast(Boolean failFast) {
			this.failFast = failFast;
		}

		public Boolean getPropagateFailures() {
			return this.propagateFailures;
		}

		public void setPropagateFailures(Boolean propagateFailures) {
			this.propagateFailures = propagateFailures;
		}

		public Boolean getPropagateStopFailures() {
			return this.propagateStopFailures;
		}

		public void setPropagateStopFailures(Boolean propagateStopFailures) {
			this.propagateStopFailures = propagateStopFailures;
		}

	}

	public static class Listener {

		/**
		 * AckMode for acknowledgements. Allowed values are RECORD, BATCH, MANUAL.
		 */
		private AckMode ackMode;

		/**
		 * SchemaType of the consumed messages.
		 */
		private SchemaType schemaType;

		/**
		 * Max number of messages in a single batch request.
		 */
		private Integer maxNumMessages = -1;

		/**
		 * Max size in a single batch request.
		 */
		private DataSize maxNumBytes = DataSize.ofMegabytes(10);

		/**
		 * Duration to wait for enough message to fill a batch request before timing out.
		 */
		private Duration batchTimeout = Duration.ofMillis(100);

		/**
		 * Whether to record observations for receive operations when the Observations API
		 * is available.
		 */
		private Boolean observationsEnabled = true;

		public AckMode getAckMode() {
			return this.ackMode;
		}

		public void setAckMode(AckMode ackMode) {
			this.ackMode = ackMode;
		}

		public SchemaType getSchemaType() {
			return this.schemaType;
		}

		public void setSchemaType(SchemaType schemaType) {
			this.schemaType = schemaType;
		}

		public Integer getMaxNumMessages() {
			return this.maxNumMessages;
		}

		public void setMaxNumMessages(Integer maxNumMessages) {
			this.maxNumMessages = maxNumMessages;
		}

		public DataSize getMaxNumBytes() {
			return this.maxNumBytes;
		}

		public void setMaxNumBytes(DataSize maxNumBytes) {
			this.maxNumBytes = maxNumBytes;
		}

		public Duration getBatchTimeout() {
			return this.batchTimeout;
		}

		public void setBatchTimeout(Duration batchTimeout) {
			this.batchTimeout = batchTimeout;
		}

		public Boolean isObservationsEnabled() {
			return this.observationsEnabled;
		}

		public void setObservationsEnabled(Boolean observationsEnabled) {
			this.observationsEnabled = observationsEnabled;
		}

	}

	public static class Admin {

		/**
		 * Pulsar web URL for the admin endpoint in the format
		 * '(http|https)://&lt;host&gt;:&lt;port&gt;'.
		 */
		private String serviceUrl = "http://localhost:8080";

		/**
		 * Fully qualified class name of the authentication plugin.
		 */
		private String authPluginClassName;

		/**
		 * Authentication parameter(s) as a JSON encoded string.
		 */
		private String authParams;

		/**
		 * Authentication parameter(s) as a map of parameter names to parameter values.
		 */
		private Map<String, String> authentication;

		/**
		 * Path to the trusted TLS certificate file.
		 */
		private String tlsTrustCertsFilePath;

		/**
		 * Path to the TLS certificate file.
		 */
		private String tlsCertificateFilePath;

		/**
		 * Path to the TLS private key file.
		 */
		private String tlsKeyFilePath;

		/**
		 * Whether the client accepts untrusted TLS certificates from the broker.
		 */
		private Boolean tlsAllowInsecureConnection = false;

		/**
		 * Whether the hostname is validated when the proxy creates a TLS connection with
		 * brokers.
		 */
		private Boolean tlsHostnameVerificationEnable = false;

		/**
		 * Enable KeyStore instead of PEM type configuration if TLS is enabled.
		 */
		private Boolean useKeyStoreTls = false;

		/**
		 * Name of the security provider used for SSL connections.
		 */
		private String sslProvider;

		/**
		 * File format of the trust store file.
		 */
		private String tlsTrustStoreType;

		/**
		 * Location of the trust store file.
		 */
		private String tlsTrustStorePath;

		/**
		 * Store password for the key store file.
		 */
		private String tlsTrustStorePassword;

		/**
		 * List of cipher suites. This is a named combination of authentication,
		 * encryption, MAC and key exchange algorithm used to negotiate the security
		 * settings for a network connection using TLS or SSL network protocol. By
		 * default, all the available cipher suites are supported.
		 */
		private Set<String> tlsCiphers;

		/**
		 * List of SSL protocols used to generate the SSLContext. Allowed values in recent
		 * JVMs are TLS, TLSv1.3, TLSv1.2 and TLSv1.1.
		 */
		private Set<String> tlsProtocols;

		/**
		 * Duration to wait for a connection to server to be established.
		 */
		private Duration connectionTimeout = Duration.ofMinutes(1);

		/**
		 * Server response read time out for any request.
		 */
		private Duration readTimeout = Duration.ofMinutes(1);

		/**
		 * Server request time out for any request.
		 */
		private Duration requestTimeout = Duration.ofMinutes(5);

		/**
		 * Certificates auto refresh time if Pulsar admin uses tls authentication.
		 */
		private Duration autoCertRefreshTime = Duration.ofMinutes(5);

		public String getServiceUrl() {
			return this.serviceUrl;
		}

		public void setServiceUrl(String serviceUrl) {
			this.serviceUrl = serviceUrl;
		}

		public String getAuthPluginClassName() {
			return this.authPluginClassName;
		}

		public void setAuthPluginClassName(String authPluginClassName) {
			this.authPluginClassName = authPluginClassName;
		}

		public String getAuthParams() {
			return this.authParams;
		}

		public void setAuthParams(String authParams) {
			this.authParams = authParams;
		}

		public Map<String, String> getAuthentication() {
			return this.authentication;
		}

		public void setAuthentication(Map<String, String> authentication) {
			this.authentication = authentication;
		}

		public String getTlsTrustCertsFilePath() {
			return this.tlsTrustCertsFilePath;
		}

		public void setTlsTrustCertsFilePath(String tlsTrustCertsFilePath) {
			this.tlsTrustCertsFilePath = tlsTrustCertsFilePath;
		}

		public String getTlsCertificateFilePath() {
			return this.tlsCertificateFilePath;
		}

		public void setTlsCertificateFilePath(String tlsCertificateFilePath) {
			this.tlsCertificateFilePath = tlsCertificateFilePath;
		}

		public String getTlsKeyFilePath() {
			return this.tlsKeyFilePath;
		}

		public void setTlsKeyFilePath(String tlsKeyFilePath) {
			this.tlsKeyFilePath = tlsKeyFilePath;
		}

		public Boolean isTlsAllowInsecureConnection() {
			return this.tlsAllowInsecureConnection;
		}

		public void setTlsAllowInsecureConnection(Boolean tlsAllowInsecureConnection) {
			this.tlsAllowInsecureConnection = tlsAllowInsecureConnection;
		}

		public Boolean isTlsHostnameVerificationEnable() {
			return this.tlsHostnameVerificationEnable;
		}

		public void setTlsHostnameVerificationEnable(Boolean tlsHostnameVerificationEnable) {
			this.tlsHostnameVerificationEnable = tlsHostnameVerificationEnable;
		}

		public Boolean isUseKeyStoreTls() {
			return this.useKeyStoreTls;
		}

		public void setUseKeyStoreTls(Boolean useKeyStoreTls) {
			this.useKeyStoreTls = useKeyStoreTls;
		}

		public String getSslProvider() {
			return this.sslProvider;
		}

		public void setSslProvider(String sslProvider) {
			this.sslProvider = sslProvider;
		}

		public String getTlsTrustStoreType() {
			return this.tlsTrustStoreType;
		}

		public void setTlsTrustStoreType(String tlsTrustStoreType) {
			this.tlsTrustStoreType = tlsTrustStoreType;
		}

		public String getTlsTrustStorePath() {
			return this.tlsTrustStorePath;
		}

		public void setTlsTrustStorePath(String tlsTrustStorePath) {
			this.tlsTrustStorePath = tlsTrustStorePath;
		}

		public String getTlsTrustStorePassword() {
			return this.tlsTrustStorePassword;
		}

		public void setTlsTrustStorePassword(String tlsTrustStorePassword) {
			this.tlsTrustStorePassword = tlsTrustStorePassword;
		}

		public Set<String> getTlsCiphers() {
			return this.tlsCiphers;
		}

		public void setTlsCiphers(Set<String> tlsCiphers) {
			this.tlsCiphers = tlsCiphers;
		}

		public Set<String> getTlsProtocols() {
			return this.tlsProtocols;
		}

		public void setTlsProtocols(Set<String> tlsProtocols) {
			this.tlsProtocols = tlsProtocols;
		}

		public Duration getConnectionTimeout() {
			return this.connectionTimeout;
		}

		public void setConnectionTimeout(Duration connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		public Duration getReadTimeout() {
			return this.readTimeout;
		}

		public void setReadTimeout(Duration readTimeout) {
			this.readTimeout = readTimeout;
		}

		public Duration getRequestTimeout() {
			return this.requestTimeout;
		}

		public void setRequestTimeout(Duration requestTimeout) {
			this.requestTimeout = requestTimeout;
		}

		public Duration getAutoCertRefreshTime() {
			return this.autoCertRefreshTime;
		}

		public void setAutoCertRefreshTime(Duration autoCertRefreshTime) {
			this.autoCertRefreshTime = autoCertRefreshTime;
		}

		public PulsarAdminBuilderCustomizer toPulsarAdminBuilderCustomizer() {
			return (adminBuilder) -> {
				PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
				map.from(this::getServiceUrl).to(adminBuilder::serviceHttpUrl);
				applyAuthentication(adminBuilder);
				map.from(this::getTlsTrustCertsFilePath).to(adminBuilder::tlsTrustCertsFilePath);
				map.from(this::getTlsCertificateFilePath).to(adminBuilder::tlsCertificateFilePath);
				map.from(this::getTlsKeyFilePath).to(adminBuilder::tlsKeyFilePath);
				map.from(this::isTlsAllowInsecureConnection).to(adminBuilder::allowTlsInsecureConnection);
				map.from(this::isTlsHostnameVerificationEnable).to(adminBuilder::enableTlsHostnameVerification);
				map.from(this::isUseKeyStoreTls).to(adminBuilder::useKeyStoreTls);
				map.from(this::getSslProvider).to(adminBuilder::sslProvider);
				map.from(this::getTlsTrustStoreType).to(adminBuilder::tlsTrustStoreType);
				map.from(this::getTlsTrustStorePath).to(adminBuilder::tlsTrustStorePath);
				map.from(this::getTlsTrustStorePassword).to(adminBuilder::tlsTrustStorePassword);
				map.from(this::getTlsCiphers).to(adminBuilder::tlsCiphers);
				map.from(this::getTlsProtocols).to(adminBuilder::tlsProtocols);
				map.from(this::getConnectionTimeout)
					.asInt(Duration::toMillis)
					.to(adminBuilder, (ab, val) -> ab.connectionTimeout(val, TimeUnit.MILLISECONDS));
				map.from(this::getReadTimeout)
					.asInt(Duration::toMillis)
					.to(adminBuilder, (ab, val) -> ab.readTimeout(val, TimeUnit.MILLISECONDS));
				map.from(this::getRequestTimeout)
					.asInt(Duration::toMillis)
					.to(adminBuilder, (ab, val) -> ab.requestTimeout(val, TimeUnit.MILLISECONDS));
				map.from(this::getAutoCertRefreshTime)
					.asInt(Duration::toMillis)
					.to(adminBuilder, (ab, val) -> ab.autoCertRefreshTime(val, TimeUnit.MILLISECONDS));
			};
		}

		private void applyAuthentication(PulsarAdminBuilder adminBuilder) {
			if (StringUtils.hasText(this.getAuthParams()) && !CollectionUtils.isEmpty(this.getAuthentication())) {
				throw new IllegalArgumentException(
						"Cannot set both spring.pulsar.administration.authParams and spring.pulsar.administration.authentication.*");
			}
			var authPluginClass = this.getAuthPluginClassName();
			if (StringUtils.hasText(authPluginClass)) {
				var authParams = this.getAuthParams();
				if (this.getAuthentication() != null) {
					authParams = AuthParameterUtils.maybeConvertToEncodedParamString(this.getAuthentication());
				}
				try {
					adminBuilder.authentication(authPluginClass, authParams);
				}
				catch (PulsarClientException.UnsupportedAuthenticationException ex) {
					throw new IllegalArgumentException("Unable to configure authentication: " + ex.getMessage(), ex);
				}
			}
		}

	}

	public static class Reader {

		/**
		 * Topic names.
		 */
		private List<String> topicNames;

		/**
		 * Size of a consumer's receiver queue.
		 */
		private Integer receiverQueueSize;

		/**
		 * Reader name.
		 */
		private String readerName;

		/**
		 * Subscription name.
		 */
		private String subscriptionName;

		/**
		 * Prefix of subscription role.
		 */
		private String subscriptionRolePrefix;

		/**
		 * Whether to read messages from a compacted topic rather than a full message
		 * backlog of a topic.
		 */
		private Boolean readCompacted;

		/**
		 * Whether the first message to be returned is the one specified by messageId.
		 */
		private Boolean resetIncludeHead;

		public List<String> getTopicNames() {
			return this.topicNames;
		}

		public void setTopicNames(List<String> topicNames) {
			this.topicNames = topicNames;
		}

		public Integer getReceiverQueueSize() {
			return this.receiverQueueSize;
		}

		public void setReceiverQueueSize(Integer receiverQueueSize) {
			this.receiverQueueSize = receiverQueueSize;
		}

		public String getReaderName() {
			return this.readerName;
		}

		public void setReaderName(String readerName) {
			this.readerName = readerName;
		}

		public String getSubscriptionName() {
			return this.subscriptionName;
		}

		public void setSubscriptionName(String subscriptionName) {
			this.subscriptionName = subscriptionName;
		}

		public String getSubscriptionRolePrefix() {
			return this.subscriptionRolePrefix;
		}

		public void setSubscriptionRolePrefix(String subscriptionRolePrefix) {
			this.subscriptionRolePrefix = subscriptionRolePrefix;
		}

		public Boolean getReadCompacted() {
			return this.readCompacted;
		}

		public void setReadCompacted(Boolean readCompacted) {
			this.readCompacted = readCompacted;
		}

		public Boolean getResetIncludeHead() {
			return this.resetIncludeHead;
		}

		public void setResetIncludeHead(Boolean resetIncludeHead) {
			this.resetIncludeHead = resetIncludeHead;
		}

		public ReaderBuilderCustomizer<?> toReaderBuilderCustomizer() {
			return (readerBuilder) -> {
				PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
				map.from(this::getTopicNames).as(ArrayList::new).to(readerBuilder::topics);
				map.from(this::getReceiverQueueSize).to(readerBuilder::receiverQueueSize);
				map.from(this::getReaderName).to(readerBuilder::readerName);
				map.from(this::getSubscriptionName).to(readerBuilder::subscriptionName);
				map.from(this::getSubscriptionRolePrefix).to(readerBuilder::subscriptionRolePrefix);
				map.from(this::getReadCompacted).to(readerBuilder::readCompacted);
				map.from(this::getResetIncludeHead).whenTrue().to((b) -> readerBuilder.startMessageIdInclusive());
			};
		}

	}

	public static class Defaults {

		/**
		 * List of mappings from message type to topic name and schema info to use as a
		 * defaults when a topic name and/or schema is not explicitly specified when
		 * producing or consuming messages of the mapped type.
		 */
		private List<TypeMapping> typeMappings = new ArrayList<>();

		public List<TypeMapping> getTypeMappings() {
			return this.typeMappings;
		}

		public void setTypeMappings(List<TypeMapping> typeMappings) {
			this.typeMappings = typeMappings;
		}

	}

	/**
	 * A mapping from message type to topic and/or schema info to use (at least one of
	 * {@code topicName} or {@code schemaInfo} must be specified.
	 *
	 * @param messageType the message type
	 * @param topicName the topic name
	 * @param schemaInfo the schema info
	 */
	public record TypeMapping(Class<?> messageType, String topicName, SchemaInfo schemaInfo) {
		public TypeMapping {
			Objects.requireNonNull(messageType, "messageType must not be null");
			if (topicName == null && schemaInfo == null) {
				throw new IllegalArgumentException("At least one of topicName or schemaInfo must not be null");
			}
		}
	}

	/**
	 * Represents a schema - holds enough information to construct an actual schema
	 * instance.
	 *
	 * @param schemaType schema type
	 * @param messageKeyType message key type (required for key value type)
	 */
	public record SchemaInfo(SchemaType schemaType, Class<?> messageKeyType) {
		public SchemaInfo {
			Objects.requireNonNull(schemaType, "schemaType must not be null");
			if (schemaType == SchemaType.NONE) {
				throw new IllegalArgumentException("schemaType NONE not supported");
			}
			if (schemaType != SchemaType.KEY_VALUE && messageKeyType != null) {
				throw new IllegalArgumentException("messageKeyType can only be set when schemaType is KEY_VALUE");
			}
		}
	}

}

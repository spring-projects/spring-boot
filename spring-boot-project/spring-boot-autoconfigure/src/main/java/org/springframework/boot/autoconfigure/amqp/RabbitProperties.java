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

package org.springframework.boot.autoconfigure.amqp;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory.AddressShuffleMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.CacheMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * Configuration properties for Rabbit.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Josh Thornhill
 * @author Gary Russell
 * @author Artsiom Yudovin
 * @author Franjo Zilic
 * @author Eddú Meléndez
 * @author Rafael Carvalho
 * @author Scott Frederick
 * @author Lasse Wulff
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.rabbitmq")
public class RabbitProperties {

	private static final int DEFAULT_PORT = 5672;

	private static final int DEFAULT_PORT_SECURE = 5671;

	private static final int DEFAULT_STREAM_PORT = 5552;

	/**
	 * RabbitMQ host. Ignored if an address is set.
	 */
	private String host = "localhost";

	/**
	 * RabbitMQ port. Ignored if an address is set. Default to 5672, or 5671 if SSL is
	 * enabled.
	 */
	private Integer port;

	/**
	 * Login user to authenticate to the broker.
	 */
	private String username = "guest";

	/**
	 * Login to authenticate against the broker.
	 */
	private String password = "guest";

	/**
	 * SSL configuration.
	 */
	private final Ssl ssl = new Ssl();

	/**
	 * Virtual host to use when connecting to the broker.
	 */
	private String virtualHost;

	/**
	 * Comma-separated list of addresses to which the client should connect. When set, the
	 * host and port are ignored.
	 */
	private String addresses;

	/**
	 * Mode used to shuffle configured addresses.
	 */
	private AddressShuffleMode addressShuffleMode = AddressShuffleMode.NONE;

	/**
	 * Requested heartbeat timeout; zero for none. If a duration suffix is not specified,
	 * seconds will be used.
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration requestedHeartbeat;

	/**
	 * Number of channels per connection requested by the client. Use 0 for unlimited.
	 */
	private int requestedChannelMax = 2047;

	/**
	 * Whether to enable publisher returns.
	 */
	private boolean publisherReturns;

	/**
	 * Type of publisher confirms to use.
	 */
	private ConfirmType publisherConfirmType;

	/**
	 * Connection timeout. Set it to zero to wait forever.
	 */
	private Duration connectionTimeout;

	/**
	 * Continuation timeout for RPC calls in channels. Set it to zero to wait forever.
	 */
	private Duration channelRpcTimeout = Duration.ofMinutes(10);

	/**
	 * Maximum size of the body of inbound (received) messages.
	 */
	private DataSize maxInboundMessageBodySize = DataSize.ofMegabytes(64);

	/**
	 * Cache configuration.
	 */
	private final Cache cache = new Cache();

	/**
	 * Listener container configuration.
	 */
	private final Listener listener = new Listener();

	private final Template template = new Template();

	private final Stream stream = new Stream();

	private List<Address> parsedAddresses;

	/**
     * Returns the host value of the RabbitProperties object.
     *
     * @return the host value of the RabbitProperties object
     */
    public String getHost() {
		return this.host;
	}

	/**
	 * Returns the host from the first address, or the configured host if no addresses
	 * have been set.
	 * @return the host
	 * @see #setAddresses(String)
	 * @see #getHost()
	 */
	public String determineHost() {
		if (CollectionUtils.isEmpty(this.parsedAddresses)) {
			return getHost();
		}
		return this.parsedAddresses.get(0).host;
	}

	/**
     * Sets the host for the RabbitProperties.
     * 
     * @param host the host to be set
     */
    public void setHost(String host) {
		this.host = host;
	}

	/**
     * Returns the port number.
     *
     * @return the port number
     */
    public Integer getPort() {
		return this.port;
	}

	/**
	 * Returns the port from the first address, or the configured port if no addresses
	 * have been set.
	 * @return the port
	 * @see #setAddresses(String)
	 * @see #getPort()
	 */
	public int determinePort() {
		if (CollectionUtils.isEmpty(this.parsedAddresses)) {
			Integer port = getPort();
			if (port != null) {
				return port;
			}
			return (Optional.ofNullable(getSsl().getEnabled()).orElse(false)) ? DEFAULT_PORT_SECURE : DEFAULT_PORT;
		}
		return this.parsedAddresses.get(0).port;
	}

	/**
     * Sets the port number for the RabbitProperties.
     * 
     * @param port the port number to be set
     */
    public void setPort(Integer port) {
		this.port = port;
	}

	/**
     * Returns the addresses of the RabbitProperties.
     *
     * @return the addresses of the RabbitProperties
     */
    public String getAddresses() {
		return this.addresses;
	}

	/**
	 * Returns the comma-separated addresses or a single address ({@code host:port})
	 * created from the configured host and port if no addresses have been set.
	 * @return the addresses
	 */
	public String determineAddresses() {
		if (CollectionUtils.isEmpty(this.parsedAddresses)) {
			if (this.host.contains(",")) {
				throw new InvalidConfigurationPropertyValueException("spring.rabbitmq.host", this.host,
						"Invalid character ','. Value must be a single host. For multiple hosts, use property 'spring.rabbitmq.addresses' instead.");
			}
			return this.host + ":" + determinePort();
		}
		List<String> addressStrings = new ArrayList<>();
		for (Address parsedAddress : this.parsedAddresses) {
			addressStrings.add(parsedAddress.host + ":" + parsedAddress.port);
		}
		return StringUtils.collectionToCommaDelimitedString(addressStrings);
	}

	/**
     * Sets the addresses for the RabbitProperties.
     * 
     * @param addresses the addresses to be set
     * 
     * @see RabbitProperties#parseAddresses(String)
     */
    public void setAddresses(String addresses) {
		this.addresses = addresses;
		this.parsedAddresses = parseAddresses(addresses);
	}

	/**
     * Parses a string of addresses and returns a list of Address objects.
     * 
     * @param addresses the string of addresses to be parsed
     * @return a list of Address objects representing the parsed addresses
     */
    private List<Address> parseAddresses(String addresses) {
		List<Address> parsedAddresses = new ArrayList<>();
		for (String address : StringUtils.commaDelimitedListToStringArray(addresses)) {
			parsedAddresses.add(new Address(address, Optional.ofNullable(getSsl().getEnabled()).orElse(false)));
		}
		return parsedAddresses;
	}

	/**
     * Returns the username associated with the RabbitProperties object.
     *
     * @return the username
     */
    public String getUsername() {
		return this.username;
	}

	/**
	 * If addresses have been set and the first address has a username it is returned.
	 * Otherwise returns the result of calling {@code getUsername()}.
	 * @return the username
	 * @see #setAddresses(String)
	 * @see #getUsername()
	 */
	public String determineUsername() {
		if (CollectionUtils.isEmpty(this.parsedAddresses)) {
			return this.username;
		}
		Address address = this.parsedAddresses.get(0);
		return (address.username != null) ? address.username : this.username;
	}

	/**
     * Sets the username for RabbitProperties.
     * 
     * @param username the username to be set
     */
    public void setUsername(String username) {
		this.username = username;
	}

	/**
     * Returns the password associated with the RabbitProperties object.
     *
     * @return the password
     */
    public String getPassword() {
		return this.password;
	}

	/**
	 * If addresses have been set and the first address has a password it is returned.
	 * Otherwise returns the result of calling {@code getPassword()}.
	 * @return the password or {@code null}
	 * @see #setAddresses(String)
	 * @see #getPassword()
	 */
	public String determinePassword() {
		if (CollectionUtils.isEmpty(this.parsedAddresses)) {
			return getPassword();
		}
		Address address = this.parsedAddresses.get(0);
		return (address.password != null) ? address.password : getPassword();
	}

	/**
     * Sets the password for the RabbitProperties.
     * 
     * @param password the password to be set
     */
    public void setPassword(String password) {
		this.password = password;
	}

	/**
     * Returns the SSL configuration for RabbitMQ.
     *
     * @return the SSL configuration for RabbitMQ
     */
    public Ssl getSsl() {
		return this.ssl;
	}

	/**
     * Returns the virtual host associated with the RabbitMQ connection.
     *
     * @return the virtual host
     */
    public String getVirtualHost() {
		return this.virtualHost;
	}

	/**
	 * If addresses have been set and the first address has a virtual host it is returned.
	 * Otherwise returns the result of calling {@code getVirtualHost()}.
	 * @return the virtual host or {@code null}
	 * @see #setAddresses(String)
	 * @see #getVirtualHost()
	 */
	public String determineVirtualHost() {
		if (CollectionUtils.isEmpty(this.parsedAddresses)) {
			return getVirtualHost();
		}
		Address address = this.parsedAddresses.get(0);
		return (address.virtualHost != null) ? address.virtualHost : getVirtualHost();
	}

	/**
     * Sets the virtual host for the RabbitMQ connection.
     * 
     * @param virtualHost the virtual host to be set
     */
    public void setVirtualHost(String virtualHost) {
		this.virtualHost = StringUtils.hasText(virtualHost) ? virtualHost : "/";
	}

	/**
     * Returns the address shuffle mode of the RabbitProperties.
     *
     * @return the address shuffle mode of the RabbitProperties
     */
    public AddressShuffleMode getAddressShuffleMode() {
		return this.addressShuffleMode;
	}

	/**
     * Sets the address shuffle mode for the RabbitProperties.
     * 
     * @param addressShuffleMode the address shuffle mode to be set
     */
    public void setAddressShuffleMode(AddressShuffleMode addressShuffleMode) {
		this.addressShuffleMode = addressShuffleMode;
	}

	/**
     * Returns the requested heartbeat duration.
     *
     * @return the requested heartbeat duration
     */
    public Duration getRequestedHeartbeat() {
		return this.requestedHeartbeat;
	}

	/**
     * Sets the requested heartbeat duration for the RabbitMQ connection.
     * 
     * @param requestedHeartbeat the requested heartbeat duration to be set
     */
    public void setRequestedHeartbeat(Duration requestedHeartbeat) {
		this.requestedHeartbeat = requestedHeartbeat;
	}

	/**
     * Returns the maximum number of requested channels.
     *
     * @return the maximum number of requested channels
     */
    public int getRequestedChannelMax() {
		return this.requestedChannelMax;
	}

	/**
     * Sets the maximum number of requested channels.
     * 
     * @param requestedChannelMax the maximum number of requested channels
     */
    public void setRequestedChannelMax(int requestedChannelMax) {
		this.requestedChannelMax = requestedChannelMax;
	}

	/**
     * Returns a boolean value indicating whether the publisher returns are enabled.
     * 
     * @return true if publisher returns are enabled, false otherwise
     */
    public boolean isPublisherReturns() {
		return this.publisherReturns;
	}

	/**
     * Sets the value indicating whether the publisher returns are enabled or not.
     * 
     * @param publisherReturns the value indicating whether the publisher returns are enabled or not
     */
    public void setPublisherReturns(boolean publisherReturns) {
		this.publisherReturns = publisherReturns;
	}

	/**
     * Returns the connection timeout duration.
     *
     * @return the connection timeout duration
     */
    public Duration getConnectionTimeout() {
		return this.connectionTimeout;
	}

	/**
     * Sets the publisher confirm type for the RabbitProperties.
     * 
     * @param publisherConfirmType the publisher confirm type to be set
     */
    public void setPublisherConfirmType(ConfirmType publisherConfirmType) {
		this.publisherConfirmType = publisherConfirmType;
	}

	/**
     * Returns the confirm type for the publisher.
     *
     * @return the confirm type for the publisher
     */
    public ConfirmType getPublisherConfirmType() {
		return this.publisherConfirmType;
	}

	/**
     * Sets the connection timeout for RabbitMQ.
     * 
     * @param connectionTimeout the connection timeout duration
     */
    public void setConnectionTimeout(Duration connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
     * Returns the channel RPC timeout.
     *
     * @return the channel RPC timeout
     */
    public Duration getChannelRpcTimeout() {
		return this.channelRpcTimeout;
	}

	/**
     * Sets the timeout for channel RPC calls.
     * 
     * @param channelRpcTimeout the timeout for channel RPC calls
     */
    public void setChannelRpcTimeout(Duration channelRpcTimeout) {
		this.channelRpcTimeout = channelRpcTimeout;
	}

	/**
     * Returns the maximum size of the inbound message body.
     *
     * @return the maximum size of the inbound message body
     */
    public DataSize getMaxInboundMessageBodySize() {
		return this.maxInboundMessageBodySize;
	}

	/**
     * Sets the maximum size of the inbound message body.
     * 
     * @param maxInboundMessageBodySize the maximum size of the inbound message body
     */
    public void setMaxInboundMessageBodySize(DataSize maxInboundMessageBodySize) {
		this.maxInboundMessageBodySize = maxInboundMessageBodySize;
	}

	/**
     * Returns the cache object associated with this RabbitProperties instance.
     *
     * @return the cache object
     */
    public Cache getCache() {
		return this.cache;
	}

	/**
     * Returns the listener associated with this RabbitProperties object.
     *
     * @return the listener associated with this RabbitProperties object
     */
    public Listener getListener() {
		return this.listener;
	}

	/**
     * Returns the template associated with the RabbitProperties.
     *
     * @return the template associated with the RabbitProperties
     */
    public Template getTemplate() {
		return this.template;
	}

	/**
     * Returns the stream associated with this RabbitProperties object.
     *
     * @return the stream associated with this RabbitProperties object
     */
    public Stream getStream() {
		return this.stream;
	}

	/**
     * Ssl class.
     */
    public class Ssl {

		private static final String SUN_X509 = "SunX509";

		/**
		 * Whether to enable SSL support. Determined automatically if an address is
		 * provided with the protocol (amqp:// vs. amqps://).
		 */
		private Boolean enabled;

		/**
		 * SSL bundle name.
		 */
		private String bundle;

		/**
		 * Path to the key store that holds the SSL certificate.
		 */
		private String keyStore;

		/**
		 * Key store type.
		 */
		private String keyStoreType = "PKCS12";

		/**
		 * Password used to access the key store.
		 */
		private String keyStorePassword;

		/**
		 * Key store algorithm.
		 */
		private String keyStoreAlgorithm = SUN_X509;

		/**
		 * Trust store that holds SSL certificates.
		 */
		private String trustStore;

		/**
		 * Trust store type.
		 */
		private String trustStoreType = "JKS";

		/**
		 * Password used to access the trust store.
		 */
		private String trustStorePassword;

		/**
		 * Trust store algorithm.
		 */
		private String trustStoreAlgorithm = SUN_X509;

		/**
		 * SSL algorithm to use. By default, configured by the Rabbit client library.
		 */
		private String algorithm;

		/**
		 * Whether to enable server side certificate validation.
		 */
		private boolean validateServerCertificate = true;

		/**
		 * Whether to enable hostname verification.
		 */
		private boolean verifyHostname = true;

		/**
         * Returns the value of the enabled property.
         *
         * @return the value of the enabled property
         */
        public Boolean getEnabled() {
			return this.enabled;
		}

		/**
		 * Returns whether SSL is enabled from the first address, or the configured ssl
		 * enabled flag if no addresses have been set.
		 * @return whether ssl is enabled
		 * @see #setAddresses(String)
		 * @see #getEnabled() ()
		 */
		public boolean determineEnabled() {
			boolean defaultEnabled = Optional.ofNullable(getEnabled()).orElse(false) || this.bundle != null;
			if (CollectionUtils.isEmpty(RabbitProperties.this.parsedAddresses)) {
				return defaultEnabled;
			}
			Address address = RabbitProperties.this.parsedAddresses.get(0);
			return address.determineSslEnabled(defaultEnabled);
		}

		/**
         * Sets the enabled status of the SSL.
         * 
         * @param enabled the enabled status to be set
         */
        public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		/**
         * Returns the bundle associated with this Ssl object.
         * 
         * @return the bundle associated with this Ssl object
         */
        public String getBundle() {
			return this.bundle;
		}

		/**
         * Sets the bundle for the Ssl class.
         * 
         * @param bundle the bundle to be set
         */
        public void setBundle(String bundle) {
			this.bundle = bundle;
		}

		/**
         * Returns the key store used by the SSL configuration.
         *
         * @return the key store used by the SSL configuration
         */
        public String getKeyStore() {
			return this.keyStore;
		}

		/**
         * Sets the path to the key store file.
         * 
         * @param keyStore the path to the key store file
         */
        public void setKeyStore(String keyStore) {
			this.keyStore = keyStore;
		}

		/**
         * Returns the type of the keystore used for SSL configuration.
         * 
         * @return the keystore type
         */
        public String getKeyStoreType() {
			return this.keyStoreType;
		}

		/**
         * Sets the type of the keystore.
         * 
         * @param keyStoreType the type of the keystore
         */
        public void setKeyStoreType(String keyStoreType) {
			this.keyStoreType = keyStoreType;
		}

		/**
         * Returns the password for the keystore.
         *
         * @return the password for the keystore
         */
        public String getKeyStorePassword() {
			return this.keyStorePassword;
		}

		/**
         * Sets the password for the key store.
         * 
         * @param keyStorePassword the password for the key store
         */
        public void setKeyStorePassword(String keyStorePassword) {
			this.keyStorePassword = keyStorePassword;
		}

		/**
         * Returns the algorithm used for the KeyStore.
         * 
         * @return the algorithm used for the KeyStore
         */
        public String getKeyStoreAlgorithm() {
			return this.keyStoreAlgorithm;
		}

		/**
         * Sets the algorithm used for the key store.
         * 
         * @param keyStoreAlgorithm the algorithm used for the key store
         */
        public void setKeyStoreAlgorithm(String keyStoreAlgorithm) {
			this.keyStoreAlgorithm = keyStoreAlgorithm;
		}

		/**
         * Returns the trust store path.
         * 
         * @return the trust store path
         */
        public String getTrustStore() {
			return this.trustStore;
		}

		/**
         * Sets the trust store file path for SSL/TLS connections.
         * 
         * @param trustStore the file path of the trust store
         */
        public void setTrustStore(String trustStore) {
			this.trustStore = trustStore;
		}

		/**
         * Returns the type of the trust store.
         * 
         * @return the trust store type
         */
        public String getTrustStoreType() {
			return this.trustStoreType;
		}

		/**
         * Sets the type of the trust store.
         * 
         * @param trustStoreType the type of the trust store
         */
        public void setTrustStoreType(String trustStoreType) {
			this.trustStoreType = trustStoreType;
		}

		/**
         * Returns the password for the trust store.
         *
         * @return the trust store password
         */
        public String getTrustStorePassword() {
			return this.trustStorePassword;
		}

		/**
         * Sets the password for the trust store.
         * 
         * @param trustStorePassword the password for the trust store
         */
        public void setTrustStorePassword(String trustStorePassword) {
			this.trustStorePassword = trustStorePassword;
		}

		/**
         * Returns the algorithm used by the trust store.
         *
         * @return the trust store algorithm
         */
        public String getTrustStoreAlgorithm() {
			return this.trustStoreAlgorithm;
		}

		/**
         * Sets the trust store algorithm for SSL connections.
         * 
         * @param trustStoreAlgorithm the trust store algorithm to be set
         */
        public void setTrustStoreAlgorithm(String trustStoreAlgorithm) {
			this.trustStoreAlgorithm = trustStoreAlgorithm;
		}

		/**
         * Returns the algorithm used by the SSL connection.
         *
         * @return the algorithm used by the SSL connection
         */
        public String getAlgorithm() {
			return this.algorithm;
		}

		/**
         * Sets the SSL algorithm to be used.
         * 
         * @param sslAlgorithm the SSL algorithm to be set
         */
        public void setAlgorithm(String sslAlgorithm) {
			this.algorithm = sslAlgorithm;
		}

		/**
         * Returns the value indicating whether the server certificate should be validated.
         * 
         * @return {@code true} if the server certificate should be validated, {@code false} otherwise.
         */
        public boolean isValidateServerCertificate() {
			return this.validateServerCertificate;
		}

		/**
         * Sets whether to validate the server certificate during SSL connection.
         * 
         * @param validateServerCertificate true to validate the server certificate, false otherwise
         */
        public void setValidateServerCertificate(boolean validateServerCertificate) {
			this.validateServerCertificate = validateServerCertificate;
		}

		/**
         * Returns the value of the verifyHostname property.
         * 
         * @return the value of the verifyHostname property
         */
        public boolean getVerifyHostname() {
			return this.verifyHostname;
		}

		/**
         * Sets whether to verify the hostname during SSL/TLS handshake.
         * 
         * @param verifyHostname true to verify the hostname, false otherwise
         */
        public void setVerifyHostname(boolean verifyHostname) {
			this.verifyHostname = verifyHostname;
		}

	}

	/**
     * Cache class.
     */
    public static class Cache {

		private final Channel channel = new Channel();

		private final Connection connection = new Connection();

		/**
         * Returns the channel associated with this cache.
         * 
         * @return the channel associated with this cache
         */
        public Channel getChannel() {
			return this.channel;
		}

		/**
         * Returns the connection object associated with this Cache instance.
         *
         * @return the connection object
         */
        public Connection getConnection() {
			return this.connection;
		}

		/**
         * Channel class.
         */
        public static class Channel {

			/**
			 * Number of channels to retain in the cache. When "check-timeout" > 0, max
			 * channels per connection.
			 */
			private Integer size;

			/**
			 * Duration to wait to obtain a channel if the cache size has been reached. If
			 * 0, always create a new channel.
			 */
			private Duration checkoutTimeout;

			/**
             * Returns the size of the Channel.
             *
             * @return the size of the Channel
             */
            public Integer getSize() {
				return this.size;
			}

			/**
             * Sets the size of the channel.
             * 
             * @param size the size of the channel to be set
             */
            public void setSize(Integer size) {
				this.size = size;
			}

			/**
             * Returns the checkout timeout duration.
             *
             * @return the checkout timeout duration
             */
            public Duration getCheckoutTimeout() {
				return this.checkoutTimeout;
			}

			/**
             * Sets the checkout timeout for the channel.
             * 
             * @param checkoutTimeout the duration of the checkout timeout
             */
            public void setCheckoutTimeout(Duration checkoutTimeout) {
				this.checkoutTimeout = checkoutTimeout;
			}

		}

		/**
         * Connection class.
         */
        public static class Connection {

			/**
			 * Connection factory cache mode.
			 */
			private CacheMode mode = CacheMode.CHANNEL;

			/**
			 * Number of connections to cache. Only applies when mode is CONNECTION.
			 */
			private Integer size;

			/**
             * Returns the cache mode of the connection.
             * 
             * @return the cache mode of the connection
             */
            public CacheMode getMode() {
				return this.mode;
			}

			/**
             * Sets the cache mode for the connection.
             * 
             * @param mode the cache mode to be set
             */
            public void setMode(CacheMode mode) {
				this.mode = mode;
			}

			/**
             * Returns the size of the Connection.
             *
             * @return the size of the Connection
             */
            public Integer getSize() {
				return this.size;
			}

			/**
             * Sets the size of the connection.
             * 
             * @param size the size of the connection to be set
             */
            public void setSize(Integer size) {
				this.size = size;
			}

		}

	}

	public enum ContainerType {

		/**
		 * Container where the RabbitMQ consumer dispatches messages to an invoker thread.
		 */
		SIMPLE,

		/**
		 * Container where the listener is invoked directly on the RabbitMQ consumer
		 * thread.
		 */
		DIRECT,

		/**
		 * Container that uses the RabbitMQ Stream Client.
		 */
		STREAM

	}

	/**
     * Listener class.
     */
    public static class Listener {

		/**
		 * Listener container type.
		 */
		private ContainerType type = ContainerType.SIMPLE;

		private final SimpleContainer simple = new SimpleContainer();

		private final DirectContainer direct = new DirectContainer();

		private final StreamContainer stream = new StreamContainer();

		/**
         * Returns the type of the container.
         * 
         * @return the type of the container
         */
        public ContainerType getType() {
			return this.type;
		}

		/**
         * Sets the type of the container.
         * 
         * @param containerType the type of the container
         */
        public void setType(ContainerType containerType) {
			this.type = containerType;
		}

		/**
         * Returns the SimpleContainer object associated with this Listener.
         *
         * @return the SimpleContainer object associated with this Listener
         */
        public SimpleContainer getSimple() {
			return this.simple;
		}

		/**
         * Returns the DirectContainer object associated with this Listener.
         *
         * @return the DirectContainer object associated with this Listener
         */
        public DirectContainer getDirect() {
			return this.direct;
		}

		/**
         * Returns the StreamContainer object associated with this Listener.
         *
         * @return the StreamContainer object associated with this Listener
         */
        public StreamContainer getStream() {
			return this.stream;
		}

	}

	/**
     * BaseContainer class.
     */
    public abstract static class BaseContainer {

		/**
		 * Whether to enable observation.
		 */
		private boolean observationEnabled;

		/**
         * Returns a boolean value indicating whether observation is enabled for the BaseContainer.
         *
         * @return true if observation is enabled, false otherwise
         */
        public boolean isObservationEnabled() {
			return this.observationEnabled;
		}

		/**
         * Sets the observation enabled flag.
         * 
         * @param observationEnabled the flag indicating whether observation is enabled or not
         */
        public void setObservationEnabled(boolean observationEnabled) {
			this.observationEnabled = observationEnabled;
		}

	}

	/**
     * AmqpContainer class.
     */
    public abstract static class AmqpContainer extends BaseContainer {

		/**
		 * Whether to start the container automatically on startup.
		 */
		private boolean autoStartup = true;

		/**
		 * Acknowledge mode of container.
		 */
		private AcknowledgeMode acknowledgeMode;

		/**
		 * Maximum number of unacknowledged messages that can be outstanding at each
		 * consumer.
		 */
		private Integer prefetch;

		/**
		 * Whether rejected deliveries are re-queued by default.
		 */
		private Boolean defaultRequeueRejected;

		/**
		 * How often idle container events should be published.
		 */
		private Duration idleEventInterval;

		/**
		 * Whether the container should present batched messages as discrete messages or
		 * call the listener with the batch.
		 */
		private boolean deBatchingEnabled = true;

		/**
		 * Whether the container (when stopped) should stop immediately after processing
		 * the current message or stop after processing all pre-fetched messages.
		 */
		private boolean forceStop;

		/**
		 * Optional properties for a retry interceptor.
		 */
		private final ListenerRetry retry = new ListenerRetry();

		/**
         * Returns a boolean value indicating whether the container is set to automatically start up.
         * 
         * @return {@code true} if the container is set to automatically start up, {@code false} otherwise
         */
        public boolean isAutoStartup() {
			return this.autoStartup;
		}

		/**
         * Sets the flag indicating whether the container should automatically start upon initialization.
         * 
         * @param autoStartup the flag indicating whether the container should automatically start
         */
        public void setAutoStartup(boolean autoStartup) {
			this.autoStartup = autoStartup;
		}

		/**
         * Returns the acknowledge mode of the AmqpContainer.
         * 
         * @return the acknowledge mode of the AmqpContainer
         */
        public AcknowledgeMode getAcknowledgeMode() {
			return this.acknowledgeMode;
		}

		/**
         * Sets the acknowledge mode for the AMQP container.
         * 
         * @param acknowledgeMode the acknowledge mode to be set
         */
        public void setAcknowledgeMode(AcknowledgeMode acknowledgeMode) {
			this.acknowledgeMode = acknowledgeMode;
		}

		/**
         * Returns the value of the prefetch property.
         *
         * @return the value of the prefetch property
         */
        public Integer getPrefetch() {
			return this.prefetch;
		}

		/**
         * Sets the prefetch value for the AmqpContainer.
         * 
         * @param prefetch the prefetch value to be set
         */
        public void setPrefetch(Integer prefetch) {
			this.prefetch = prefetch;
		}

		/**
         * Returns the value of the defaultRequeueRejected property.
         * 
         * @return the value of the defaultRequeueRejected property
         */
        public Boolean getDefaultRequeueRejected() {
			return this.defaultRequeueRejected;
		}

		/**
         * Sets the flag indicating whether rejected messages should be requeued by default.
         * 
         * @param defaultRequeueRejected the flag indicating whether rejected messages should be requeued by default
         */
        public void setDefaultRequeueRejected(Boolean defaultRequeueRejected) {
			this.defaultRequeueRejected = defaultRequeueRejected;
		}

		/**
         * Returns the idle event interval.
         * 
         * @return the idle event interval
         */
        public Duration getIdleEventInterval() {
			return this.idleEventInterval;
		}

		/**
         * Sets the interval at which idle events are triggered.
         * 
         * @param idleEventInterval the duration between idle events
         */
        public void setIdleEventInterval(Duration idleEventInterval) {
			this.idleEventInterval = idleEventInterval;
		}

		/**
         * Returns a boolean value indicating whether missing queues are considered fatal errors.
         * If this method returns true, the container will throw an exception if a message is sent to a non-existent queue.
         * If this method returns false, the container will silently ignore the message and continue processing.
         *
         * @return true if missing queues are considered fatal errors, false otherwise
         */
        public abstract boolean isMissingQueuesFatal();

		/**
         * Returns a boolean value indicating whether de-batching is enabled.
         * 
         * @return true if de-batching is enabled, false otherwise
         */
        public boolean isDeBatchingEnabled() {
			return this.deBatchingEnabled;
		}

		/**
         * Sets the flag indicating whether de-batching is enabled.
         * 
         * @param deBatchingEnabled true if de-batching is enabled, false otherwise
         */
        public void setDeBatchingEnabled(boolean deBatchingEnabled) {
			this.deBatchingEnabled = deBatchingEnabled;
		}

		/**
         * Returns a boolean value indicating whether the container is set to force stop.
         * 
         * @return true if the container is set to force stop, false otherwise
         */
        public boolean isForceStop() {
			return this.forceStop;
		}

		/**
         * Sets the flag indicating whether the container should force stop.
         * 
         * @param forceStop true if the container should force stop, false otherwise
         */
        public void setForceStop(boolean forceStop) {
			this.forceStop = forceStop;
		}

		/**
         * Returns the retry listener associated with this AmqpContainer.
         *
         * @return the retry listener
         */
        public ListenerRetry getRetry() {
			return this.retry;
		}

	}

	/**
	 * Configuration properties for {@code SimpleMessageListenerContainer}.
	 */
	public static class SimpleContainer extends AmqpContainer {

		/**
		 * Minimum number of listener invoker threads.
		 */
		private Integer concurrency;

		/**
		 * Maximum number of listener invoker threads.
		 */
		private Integer maxConcurrency;

		/**
		 * Batch size, expressed as the number of physical messages, to be used by the
		 * container.
		 */
		private Integer batchSize;

		/**
		 * Whether to fail if the queues declared by the container are not available on
		 * the broker and/or whether to stop the container if one or more queues are
		 * deleted at runtime.
		 */
		private boolean missingQueuesFatal = true;

		/**
		 * Whether the container creates a batch of messages based on the
		 * 'receive-timeout' and 'batch-size'. Coerces 'de-batching-enabled' to true to
		 * include the contents of a producer created batch in the batch as discrete
		 * records.
		 */
		private boolean consumerBatchEnabled;

		/**
         * Returns the concurrency level of the SimpleContainer.
         *
         * @return the concurrency level of the SimpleContainer
         */
        public Integer getConcurrency() {
			return this.concurrency;
		}

		/**
         * Sets the concurrency level for the SimpleContainer.
         * 
         * @param concurrency the concurrency level to be set
         */
        public void setConcurrency(Integer concurrency) {
			this.concurrency = concurrency;
		}

		/**
         * Returns the maximum concurrency level allowed for the container.
         *
         * @return the maximum concurrency level
         */
        public Integer getMaxConcurrency() {
			return this.maxConcurrency;
		}

		/**
         * Sets the maximum concurrency level for the container.
         * 
         * @param maxConcurrency the maximum concurrency level to be set
         */
        public void setMaxConcurrency(Integer maxConcurrency) {
			this.maxConcurrency = maxConcurrency;
		}

		/**
         * Returns the batch size of the SimpleContainer.
         *
         * @return the batch size of the SimpleContainer
         */
        public Integer getBatchSize() {
			return this.batchSize;
		}

		/**
         * Sets the batch size for processing elements in the container.
         * 
         * @param batchSize the batch size to be set
         */
        public void setBatchSize(Integer batchSize) {
			this.batchSize = batchSize;
		}

		/**
         * Returns a boolean value indicating whether missing queues are considered fatal.
         *
         * @return true if missing queues are considered fatal, false otherwise
         */
        @Override
		public boolean isMissingQueuesFatal() {
			return this.missingQueuesFatal;
		}

		/**
         * Sets whether missing queues should be treated as fatal errors.
         * 
         * @param missingQueuesFatal true if missing queues should be treated as fatal errors, false otherwise
         */
        public void setMissingQueuesFatal(boolean missingQueuesFatal) {
			this.missingQueuesFatal = missingQueuesFatal;
		}

		/**
         * Returns the status of the consumer batch.
         * 
         * @return true if the consumer batch is enabled, false otherwise.
         */
        public boolean isConsumerBatchEnabled() {
			return this.consumerBatchEnabled;
		}

		/**
         * Sets the flag indicating whether consumer batch is enabled or not.
         * 
         * @param consumerBatchEnabled the flag indicating whether consumer batch is enabled or not
         */
        public void setConsumerBatchEnabled(boolean consumerBatchEnabled) {
			this.consumerBatchEnabled = consumerBatchEnabled;
		}

	}

	/**
	 * Configuration properties for {@code DirectMessageListenerContainer}.
	 */
	public static class DirectContainer extends AmqpContainer {

		/**
		 * Number of consumers per queue.
		 */
		private Integer consumersPerQueue;

		/**
		 * Whether to fail if the queues declared by the container are not available on
		 * the broker.
		 */
		private boolean missingQueuesFatal = false;

		/**
         * Returns the number of consumers per queue.
         *
         * @return the number of consumers per queue
         */
        public Integer getConsumersPerQueue() {
			return this.consumersPerQueue;
		}

		/**
         * Sets the number of consumers per queue.
         * 
         * @param consumersPerQueue the number of consumers per queue
         */
        public void setConsumersPerQueue(Integer consumersPerQueue) {
			this.consumersPerQueue = consumersPerQueue;
		}

		/**
         * Returns a boolean value indicating whether missing queues are considered fatal.
         * 
         * @return true if missing queues are considered fatal, false otherwise
         */
        @Override
		public boolean isMissingQueuesFatal() {
			return this.missingQueuesFatal;
		}

		/**
         * Sets the flag indicating whether missing queues should be treated as fatal errors.
         * 
         * @param missingQueuesFatal the flag indicating whether missing queues should be treated as fatal errors
         */
        public void setMissingQueuesFatal(boolean missingQueuesFatal) {
			this.missingQueuesFatal = missingQueuesFatal;
		}

	}

	/**
     * StreamContainer class.
     */
    public static class StreamContainer extends BaseContainer {

		/**
		 * Whether the container will support listeners that consume native stream
		 * messages instead of Spring AMQP messages.
		 */
		private boolean nativeListener;

		/**
         * Returns a boolean value indicating whether the listener is native.
         * 
         * @return true if the listener is native, false otherwise
         */
        public boolean isNativeListener() {
			return this.nativeListener;
		}

		/**
         * Sets the flag indicating whether a native listener is used.
         * 
         * @param nativeListener the flag indicating whether a native listener is used
         */
        public void setNativeListener(boolean nativeListener) {
			this.nativeListener = nativeListener;
		}

	}

	/**
     * Template class.
     */
    public static class Template {

		private final Retry retry = new Retry();

		/**
		 * Whether to enable mandatory messages.
		 */
		private Boolean mandatory;

		/**
		 * Timeout for receive() operations.
		 */
		private Duration receiveTimeout;

		/**
		 * Timeout for sendAndReceive() operations.
		 */
		private Duration replyTimeout;

		/**
		 * Name of the default exchange to use for send operations.
		 */
		private String exchange = "";

		/**
		 * Value of a default routing key to use for send operations.
		 */
		private String routingKey = "";

		/**
		 * Name of the default queue to receive messages from when none is specified
		 * explicitly.
		 */
		private String defaultReceiveQueue;

		/**
		 * Whether to enable observation.
		 */
		private boolean observationEnabled;

		/**
         * Returns the Retry object associated with this Template.
         *
         * @return the Retry object associated with this Template
         */
        public Retry getRetry() {
			return this.retry;
		}

		/**
         * Returns the value indicating whether the field is mandatory or not.
         * 
         * @return true if the field is mandatory, false otherwise
         */
        public Boolean getMandatory() {
			return this.mandatory;
		}

		/**
         * Sets the mandatory flag for the template.
         * 
         * @param mandatory the value to set for the mandatory flag
         */
        public void setMandatory(Boolean mandatory) {
			this.mandatory = mandatory;
		}

		/**
         * Returns the receive timeout duration.
         *
         * @return the receive timeout duration
         */
        public Duration getReceiveTimeout() {
			return this.receiveTimeout;
		}

		/**
         * Sets the receive timeout for the Template.
         * 
         * @param receiveTimeout the receive timeout duration to be set
         */
        public void setReceiveTimeout(Duration receiveTimeout) {
			this.receiveTimeout = receiveTimeout;
		}

		/**
         * Returns the reply timeout duration.
         *
         * @return the reply timeout duration
         */
        public Duration getReplyTimeout() {
			return this.replyTimeout;
		}

		/**
         * Sets the reply timeout for the Template.
         * 
         * @param replyTimeout the duration of the reply timeout
         */
        public void setReplyTimeout(Duration replyTimeout) {
			this.replyTimeout = replyTimeout;
		}

		/**
         * Returns the exchange value.
         * 
         * @return the exchange value
         */
        public String getExchange() {
			return this.exchange;
		}

		/**
         * Sets the exchange for the Template.
         * 
         * @param exchange the exchange to be set
         */
        public void setExchange(String exchange) {
			this.exchange = exchange;
		}

		/**
         * Returns the routing key.
         *
         * @return the routing key
         */
        public String getRoutingKey() {
			return this.routingKey;
		}

		/**
         * Sets the routing key for the Template.
         * 
         * @param routingKey the routing key to be set
         */
        public void setRoutingKey(String routingKey) {
			this.routingKey = routingKey;
		}

		/**
         * Returns the default receive queue.
         * 
         * @return the default receive queue
         */
        public String getDefaultReceiveQueue() {
			return this.defaultReceiveQueue;
		}

		/**
         * Sets the default receive queue for the Template class.
         * 
         * @param defaultReceiveQueue the name of the default receive queue
         */
        public void setDefaultReceiveQueue(String defaultReceiveQueue) {
			this.defaultReceiveQueue = defaultReceiveQueue;
		}

		/**
         * Returns a boolean value indicating whether observation is enabled.
         * 
         * @return true if observation is enabled, false otherwise
         */
        public boolean isObservationEnabled() {
			return this.observationEnabled;
		}

		/**
         * Sets the observation enabled flag.
         * 
         * @param observationEnabled the flag indicating whether observation is enabled or not
         */
        public void setObservationEnabled(boolean observationEnabled) {
			this.observationEnabled = observationEnabled;
		}

	}

	/**
     * Retry class.
     */
    public static class Retry {

		/**
		 * Whether publishing retries are enabled.
		 */
		private boolean enabled;

		/**
		 * Maximum number of attempts to deliver a message.
		 */
		private int maxAttempts = 3;

		/**
		 * Duration between the first and second attempt to deliver a message.
		 */
		private Duration initialInterval = Duration.ofMillis(1000);

		/**
		 * Multiplier to apply to the previous retry interval.
		 */
		private double multiplier = 1.0;

		/**
		 * Maximum duration between attempts.
		 */
		private Duration maxInterval = Duration.ofMillis(10000);

		/**
         * Returns the current status of the Retry feature.
         * 
         * @return true if the Retry feature is enabled, false otherwise.
         */
        public boolean isEnabled() {
			return this.enabled;
		}

		/**
         * Sets the enabled status of the Retry.
         * 
         * @param enabled the enabled status to be set
         */
        public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		/**
         * Returns the maximum number of attempts allowed.
         *
         * @return the maximum number of attempts
         */
        public int getMaxAttempts() {
			return this.maxAttempts;
		}

		/**
         * Sets the maximum number of attempts for retrying an operation.
         * 
         * @param maxAttempts the maximum number of attempts to be set
         */
        public void setMaxAttempts(int maxAttempts) {
			this.maxAttempts = maxAttempts;
		}

		/**
         * Returns the initial interval for retrying an operation.
         *
         * @return the initial interval for retrying an operation
         */
        public Duration getInitialInterval() {
			return this.initialInterval;
		}

		/**
         * Sets the initial interval for retrying.
         * 
         * @param initialInterval the initial interval duration for retrying
         */
        public void setInitialInterval(Duration initialInterval) {
			this.initialInterval = initialInterval;
		}

		/**
         * Returns the multiplier value.
         * 
         * @return the multiplier value
         */
        public double getMultiplier() {
			return this.multiplier;
		}

		/**
         * Sets the multiplier for retrying.
         * 
         * @param multiplier the multiplier value to set
         */
        public void setMultiplier(double multiplier) {
			this.multiplier = multiplier;
		}

		/**
         * Returns the maximum interval between retries.
         *
         * @return the maximum interval between retries
         */
        public Duration getMaxInterval() {
			return this.maxInterval;
		}

		/**
         * Sets the maximum interval between retries.
         * 
         * @param maxInterval the maximum interval between retries
         */
        public void setMaxInterval(Duration maxInterval) {
			this.maxInterval = maxInterval;
		}

	}

	/**
     * ListenerRetry class.
     */
    public static class ListenerRetry extends Retry {

		/**
		 * Whether retries are stateless or stateful.
		 */
		private boolean stateless = true;

		/**
         * Returns whether the listener is stateless.
         * 
         * @return true if the listener is stateless, false otherwise
         */
        public boolean isStateless() {
			return this.stateless;
		}

		/**
         * Sets the stateless flag for the ListenerRetry class.
         * 
         * @param stateless the boolean value indicating whether the ListenerRetry class is stateless or not
         */
        public void setStateless(boolean stateless) {
			this.stateless = stateless;
		}

	}

	/**
     * Address class.
     */
    private static final class Address {

		private static final String PREFIX_AMQP = "amqp://";

		private static final String PREFIX_AMQP_SECURE = "amqps://";

		private String host;

		private int port;

		private String username;

		private String password;

		private String virtualHost;

		private Boolean secureConnection;

		/**
         * Constructs a new Address object with the given input and SSL enabled flag.
         * 
         * @param input the input string representing the address
         * @param sslEnabled true if SSL is enabled, false otherwise
         */
        private Address(String input, boolean sslEnabled) {
			input = input.trim();
			input = trimPrefix(input);
			input = parseUsernameAndPassword(input);
			input = parseVirtualHost(input);
			parseHostAndPort(input, sslEnabled);
		}

		/**
         * Trims the prefix from the given input string and sets the secureConnection flag accordingly.
         * 
         * @param input the input string to be trimmed
         * @return the trimmed input string
         */
        private String trimPrefix(String input) {
			if (input.startsWith(PREFIX_AMQP_SECURE)) {
				this.secureConnection = true;
				return input.substring(PREFIX_AMQP_SECURE.length());
			}
			if (input.startsWith(PREFIX_AMQP)) {
				this.secureConnection = false;
				return input.substring(PREFIX_AMQP.length());
			}
			return input;
		}

		/**
         * Parses the input string to extract the username and password.
         * 
         * @param input the input string containing the username and password
         * @return the remaining part of the input string after extracting the username and password
         */
        private String parseUsernameAndPassword(String input) {
			String[] splitInput = StringUtils.split(input, "@");
			if (splitInput == null) {
				return input;
			}
			String credentials = splitInput[0];
			String[] splitCredentials = StringUtils.split(credentials, ":");
			if (splitCredentials == null) {
				this.username = credentials;
			}
			else {
				this.username = splitCredentials[0];
				this.password = splitCredentials[1];
			}
			return splitInput[1];
		}

		/**
         * Parses the virtual host from the given input string.
         * 
         * @param input the input string containing the virtual host
         * @return the input string without the virtual host
         */
        private String parseVirtualHost(String input) {
			int hostIndex = input.indexOf('/');
			if (hostIndex >= 0) {
				this.virtualHost = input.substring(hostIndex + 1);
				if (this.virtualHost.isEmpty()) {
					this.virtualHost = "/";
				}
				input = input.substring(0, hostIndex);
			}
			return input;
		}

		/**
         * Parses the host and port from the given input string.
         * 
         * @param input       the input string containing the host and port information
         * @param sslEnabled  a boolean indicating whether SSL is enabled
         */
        private void parseHostAndPort(String input, boolean sslEnabled) {
			int bracketIndex = input.lastIndexOf(']');
			int colonIndex = input.lastIndexOf(':');
			if (colonIndex == -1 || colonIndex < bracketIndex) {
				this.host = input;
				this.port = (determineSslEnabled(sslEnabled)) ? DEFAULT_PORT_SECURE : DEFAULT_PORT;
			}
			else {
				this.host = input.substring(0, colonIndex);
				this.port = Integer.parseInt(input.substring(colonIndex + 1));
			}
		}

		/**
         * Determines if SSL is enabled for the address.
         * 
         * @param sslEnabled a boolean indicating if SSL is enabled
         * @return true if SSL is enabled, false otherwise
         */
        private boolean determineSslEnabled(boolean sslEnabled) {
			return (this.secureConnection != null) ? this.secureConnection : sslEnabled;
		}

	}

	/**
     * Stream class.
     */
    public static final class Stream {

		/**
		 * Host of a RabbitMQ instance with the Stream plugin enabled.
		 */
		private String host = "localhost";

		/**
		 * Stream port of a RabbitMQ instance with the Stream plugin enabled.
		 */
		private int port = DEFAULT_STREAM_PORT;

		/**
		 * Virtual host of a RabbitMQ instance with the Stream plugin enabled. When not
		 * set, spring.rabbitmq.virtual-host is used.
		 */
		private String virtualHost;

		/**
		 * Login user to authenticate to the broker. When not set,
		 * spring.rabbitmq.username is used.
		 */
		private String username;

		/**
		 * Login password to authenticate to the broker. When not set
		 * spring.rabbitmq.password is used.
		 */
		private String password;

		/**
		 * Name of the stream.
		 */
		private String name;

		/**
         * Returns the host of the Stream.
         *
         * @return the host of the Stream
         */
        public String getHost() {
			return this.host;
		}

		/**
         * Sets the host for the Stream.
         * 
         * @param host the host to be set
         */
        public void setHost(String host) {
			this.host = host;
		}

		/**
         * Returns the port number associated with this Stream.
         *
         * @return the port number
         */
        public int getPort() {
			return this.port;
		}

		/**
         * Sets the port number for the Stream.
         * 
         * @param port the port number to be set
         */
        public void setPort(int port) {
			this.port = port;
		}

		/**
         * Returns the virtual host of the Stream.
         *
         * @return the virtual host of the Stream
         */
        public String getVirtualHost() {
			return this.virtualHost;
		}

		/**
         * Sets the virtual host for the stream.
         * 
         * @param virtualHost the virtual host to be set
         */
        public void setVirtualHost(String virtualHost) {
			this.virtualHost = virtualHost;
		}

		/**
         * Returns the username associated with this Stream object.
         *
         * @return the username associated with this Stream object
         */
        public String getUsername() {
			return this.username;
		}

		/**
         * Sets the username for the Stream.
         * 
         * @param username the username to be set
         */
        public void setUsername(String username) {
			this.username = username;
		}

		/**
         * Returns the password of the Stream.
         *
         * @return the password of the Stream
         */
        public String getPassword() {
			return this.password;
		}

		/**
         * Sets the password for the Stream.
         * 
         * @param password the password to be set
         */
        public void setPassword(String password) {
			this.password = password;
		}

		/**
         * Returns the name of the Stream.
         *
         * @return the name of the Stream
         */
        public String getName() {
			return this.name;
		}

		/**
         * Sets the name of the Stream.
         * 
         * @param name the name to be set for the Stream
         */
        public void setName(String name) {
			this.name = name;
		}

	}

}

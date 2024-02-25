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

package org.springframework.boot.autoconfigure.rsocket;

import java.net.InetAddress;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.web.server.Ssl;
import org.springframework.util.unit.DataSize;

/**
 * {@link ConfigurationProperties Properties} for RSocket support.
 *
 * @author Brian Clozel
 * @author Chris Bono
 * @since 2.2.0
 */
@ConfigurationProperties("spring.rsocket")
public class RSocketProperties {

	@NestedConfigurationProperty
	private final Server server = new Server();

	/**
	 * Returns the server instance.
	 * @return the server instance
	 */
	public Server getServer() {
		return this.server;
	}

	/**
	 * Server class.
	 */
	public static class Server {

		/**
		 * Server port.
		 */
		private Integer port;

		/**
		 * Network address to which the server should bind.
		 */
		private InetAddress address;

		/**
		 * RSocket transport protocol.
		 */
		private RSocketServer.Transport transport = RSocketServer.Transport.TCP;

		/**
		 * Path under which RSocket handles requests (only works with websocket
		 * transport).
		 */
		private String mappingPath;

		/**
		 * Maximum transmission unit. Frames larger than the specified value are
		 * fragmented.
		 */
		private DataSize fragmentSize;

		@NestedConfigurationProperty
		private Ssl ssl;

		private final Spec spec = new Spec();

		/**
		 * Returns the port number of the server.
		 * @return the port number of the server
		 */
		public Integer getPort() {
			return this.port;
		}

		/**
		 * Sets the port number for the server.
		 * @param port the port number to be set
		 */
		public void setPort(Integer port) {
			this.port = port;
		}

		/**
		 * Returns the InetAddress of the server.
		 * @return the InetAddress of the server
		 */
		public InetAddress getAddress() {
			return this.address;
		}

		/**
		 * Sets the address of the server.
		 * @param address the InetAddress object representing the address of the server
		 */
		public void setAddress(InetAddress address) {
			this.address = address;
		}

		/**
		 * Returns the transport used by the RSocket server.
		 * @return the transport used by the RSocket server
		 */
		public RSocketServer.Transport getTransport() {
			return this.transport;
		}

		/**
		 * Sets the transport for the RSocketServer.
		 * @param transport the transport to be set
		 */
		public void setTransport(RSocketServer.Transport transport) {
			this.transport = transport;
		}

		/**
		 * Returns the mapping path of the server.
		 * @return the mapping path of the server
		 */
		public String getMappingPath() {
			return this.mappingPath;
		}

		/**
		 * Sets the mapping path for the server.
		 * @param mappingPath the mapping path to be set
		 */
		public void setMappingPath(String mappingPath) {
			this.mappingPath = mappingPath;
		}

		/**
		 * Returns the fragment size of the server.
		 * @return the fragment size of the server
		 */
		public DataSize getFragmentSize() {
			return this.fragmentSize;
		}

		/**
		 * Sets the fragment size for the server.
		 * @param fragmentSize the fragment size to be set
		 */
		public void setFragmentSize(DataSize fragmentSize) {
			this.fragmentSize = fragmentSize;
		}

		/**
		 * Returns the SSL object associated with this Server.
		 * @return the SSL object
		 */
		public Ssl getSsl() {
			return this.ssl;
		}

		/**
		 * Sets the SSL configuration for the server.
		 * @param ssl the SSL configuration to set
		 */
		public void setSsl(Ssl ssl) {
			this.ssl = ssl;
		}

		/**
		 * Returns the specification of the server.
		 * @return the specification of the server
		 */
		public Spec getSpec() {
			return this.spec;
		}

		/**
		 * Spec class.
		 */
		public static class Spec {

			/**
			 * Sub-protocols to use in websocket handshake signature.
			 */
			private String protocols;

			/**
			 * Maximum allowable frame payload length.
			 */
			private DataSize maxFramePayloadLength = DataSize.ofBytes(65536);

			/**
			 * Whether to proxy websocket ping frames or respond to them.
			 */
			private boolean handlePing;

			/**
			 * Whether the websocket compression extension is enabled.
			 */
			private boolean compress;

			/**
			 * Returns the protocols supported by the Spec.
			 * @return the protocols supported by the Spec
			 */
			public String getProtocols() {
				return this.protocols;
			}

			/**
			 * Sets the protocols for the Spec.
			 * @param protocols the protocols to be set
			 */
			public void setProtocols(String protocols) {
				this.protocols = protocols;
			}

			/**
			 * Returns the maximum frame payload length.
			 * @return the maximum frame payload length
			 */
			public DataSize getMaxFramePayloadLength() {
				return this.maxFramePayloadLength;
			}

			/**
			 * Sets the maximum frame payload length.
			 * @param maxFramePayloadLength the maximum frame payload length to be set
			 */
			public void setMaxFramePayloadLength(DataSize maxFramePayloadLength) {
				this.maxFramePayloadLength = maxFramePayloadLength;
			}

			/**
			 * Returns a boolean value indicating whether the handlePing flag is set.
			 * @return true if the handlePing flag is set, false otherwise.
			 */
			public boolean isHandlePing() {
				return this.handlePing;
			}

			/**
			 * Sets whether to handle ping.
			 * @param handlePing true to handle ping, false otherwise
			 */
			public void setHandlePing(boolean handlePing) {
				this.handlePing = handlePing;
			}

			/**
			 * Returns a boolean value indicating whether compression is enabled.
			 * @return true if compression is enabled, false otherwise
			 */
			public boolean isCompress() {
				return this.compress;
			}

			/**
			 * Sets the compress flag.
			 * @param compress the value to set for the compress flag
			 */
			public void setCompress(boolean compress) {
				this.compress = compress;
			}

		}

	}

}

/*
 * Copyright 2016-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.grpc.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for gRPC server.
 * @author Ray Tsang
 */
@ConfigurationProperties("grpc.server")
public class GrpcServerProperties {
	/**
	 * Server port to listen on. Defaults to 9443.
	 */
	private int port = 9443;

	/**
	 * Bind address for the server. Defaults to 0.0.0.0.
	 */
	private String address = "0.0.0.0";

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getAddress() {
		return this.address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
}

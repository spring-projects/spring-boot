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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;

import javax.net.ssl.SSLParameters;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.http.client.reactive.JdkClientHttpConnector;

/**
 * {@link ClientHttpConnectorFactory} for {@link JdkClientHttpConnector}.
 *
 * @author Phillip Webb
 */
class JdkClientHttpConnectorFactory implements ClientHttpConnectorFactory<JdkClientHttpConnector> {

	@Override
	public JdkClientHttpConnector createClientHttpConnector(SslBundle sslBundle) {
		Builder builder = HttpClient.newBuilder();
		if (sslBundle != null) {
			SslOptions options = sslBundle.getOptions();
			builder.sslContext(sslBundle.createSslContext());
			SSLParameters parameters = new SSLParameters();
			parameters.setCipherSuites(options.getCiphers());
			parameters.setProtocols(options.getEnabledProtocols());
			builder.sslParameters(parameters);
		}
		return new JdkClientHttpConnector(builder.build());
	}

}

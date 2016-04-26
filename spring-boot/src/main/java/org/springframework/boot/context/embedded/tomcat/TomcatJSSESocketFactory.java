/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.embedded.tomcat;

import java.io.IOException;
import java.security.KeyStore;

import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.jsse.JSSESocketFactory;

/**
 * {@link JSSESocketFactory} extension with capabilities to extract {@link KeyStore
 * keyStore} and {@link KeyStore trustStore} instances when the underlying
 * {@link AbstractEndpoint} instance is of {@link TomcatNioEndpoint} type.
 * @author Venil Noronha
 */
public class TomcatJSSESocketFactory extends JSSESocketFactory {

	private AbstractEndpoint<?> endpoint;

	public TomcatJSSESocketFactory(AbstractEndpoint<?> endpoint) {
		super(endpoint);
		this.endpoint = endpoint;
	}

	@Override
	protected KeyStore getKeystore(String type, String provider, String pass)
			throws IOException {
		if (this.endpoint instanceof TomcatNioEndpoint) {
			KeyStore keyStore = ((TomcatNioEndpoint) this.endpoint).getKeyStore();
			if (keyStore != null) {
				return keyStore;
			}
		}
		return super.getKeystore(type, provider, pass);
	}

	@Override
	protected KeyStore getTrustStore(String keystoreType, String keystoreProvider)
			throws IOException {
		if (this.endpoint instanceof TomcatNioEndpoint) {
			KeyStore trustStore = ((TomcatNioEndpoint) this.endpoint).getTrustStore();
			if (trustStore != null) {
				return trustStore;
			}
		}
		return super.getTrustStore(keystoreType, keystoreProvider);
	}

}

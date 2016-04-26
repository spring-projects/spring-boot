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

import java.security.KeyStore;

import org.apache.coyote.http11.Constants;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.NioEndpoint;

/**
 * {@link Http11NioProtocol} extension for providing {@link KeyStore keyStore} and
 * {@link KeyStore trustStore} instances by creating a {@link TomcatNioEndpoint} endpoint.
 * @author Venil Noronha
 */
public class TomcatHttp11NioProtocol extends Http11NioProtocol implements StoreAware {

	public TomcatHttp11NioProtocol() {
		super();
		this.endpoint = new TomcatNioEndpoint();
		((NioEndpoint) this.endpoint).setHandler((Http11ConnectionHandler) getHandler());
		setSoLinger(Constants.DEFAULT_CONNECTION_LINGER);
		setSoTimeout(Constants.DEFAULT_CONNECTION_TIMEOUT);
		setTcpNoDelay(Constants.DEFAULT_TCP_NO_DELAY);
		setSslImplementationName(TomcatJSSEImplementation.class.getName());
	}

	@Override
	public void setKeyStore(KeyStore keyStore) {
		((TomcatNioEndpoint) this.endpoint).setKeyStore(keyStore);
	}

	@Override
	public void setTrustStore(KeyStore trustStore) {
		((TomcatNioEndpoint) this.endpoint).setTrustStore(trustStore);
	}

}

/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.cli.compiler.grape;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.AuthenticationDigest;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * (Copied from aether source code - not available yet in Maven repo.)
 * 
 * @author dsyer
 */
public final class JreProxySelector implements ProxySelector {

	/**
	 * Creates a new proxy selector that delegates to
	 * {@link java.net.ProxySelector#getDefault()}.
	 */
	public JreProxySelector() {
	}

	public Proxy getProxy(RemoteRepository repository) {
		List<java.net.Proxy> proxies = null;
		try {
			URI uri = new URI(repository.getUrl()).parseServerAuthority();
			proxies = java.net.ProxySelector.getDefault().select(uri);
		}
		catch (Exception e) {
			// URL invalid or not accepted by selector or no selector at all, simply use
			// no proxy
		}
		if (proxies != null) {
			for (java.net.Proxy proxy : proxies) {
				if (java.net.Proxy.Type.DIRECT.equals(proxy.type())) {
					break;
				}
				if (java.net.Proxy.Type.HTTP.equals(proxy.type())
						&& isValid(proxy.address())) {
					InetSocketAddress addr = (InetSocketAddress) proxy.address();
					return new Proxy(Proxy.TYPE_HTTP, addr.getHostName(), addr.getPort(),
							JreProxyAuthentication.INSTANCE);
				}
			}
		}
		return null;
	}

	private static boolean isValid(SocketAddress address) {
		if (address instanceof InetSocketAddress) {
			/*
			 * NOTE: On some platforms with java.net.useSystemProxies=true, unconfigured
			 * proxies show up as proxy objects with empty host and port 0.
			 */
			InetSocketAddress addr = (InetSocketAddress) address;
			if (addr.getPort() <= 0) {
				return false;
			}
			if (addr.getHostName() == null || addr.getHostName().length() <= 0) {
				return false;
			}
			return true;
		}
		return false;
	}

	private static final class JreProxyAuthentication implements Authentication {

		public static final Authentication INSTANCE = new JreProxyAuthentication();

		public void fill(AuthenticationContext context, String key,
				Map<String, String> data) {
			Proxy proxy = context.getProxy();
			if (proxy == null) {
				return;
			}
			if (!AuthenticationContext.USERNAME.equals(key)
					&& !AuthenticationContext.PASSWORD.equals(key)) {
				return;
			}

			try {
				URL url;
				try {
					url = new URL(context.getRepository().getUrl());
				}
				catch (Exception e) {
					url = null;
				}

				PasswordAuthentication auth = Authenticator
						.requestPasswordAuthentication(proxy.getHost(), null,
								proxy.getPort(), "http",
								"Credentials for proxy " + proxy, null, url,
								Authenticator.RequestorType.PROXY);
				if (auth != null) {
					context.put(AuthenticationContext.USERNAME, auth.getUserName());
					context.put(AuthenticationContext.PASSWORD, auth.getPassword());
				}
				else {
					context.put(AuthenticationContext.USERNAME,
							System.getProperty("http.proxyUser"));
					context.put(AuthenticationContext.PASSWORD,
							System.getProperty("http.proxyPassword"));
				}
			}
			catch (SecurityException e) {
				// oh well, let's hope the proxy can do without auth
			}
		}

		public void digest(AuthenticationDigest digest) {
			// we don't know anything about the JRE's current authenticator, assume the
			// worst (i.e. interactive)
			digest.update(UUID.randomUUID().toString());
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || (obj != null && getClass().equals(obj.getClass()));
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

	}

}

/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.client.autoconfigure;

import javax.net.ssl.TrustManagerFactory;

import io.grpc.ChannelCredentials;
import io.grpc.InsecureChannelCredentials;
import io.grpc.TlsChannelCredentials;

import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.ChannelConfig;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.client.NegotiationType;
import org.springframework.grpc.internal.InsecureTrustManagerFactory;
import org.springframework.util.Assert;

/**
 * Provides channel credentials using channel configuration and {@link SslBundles}.
 *
 * @author David Syer
 * @since 4.0.0
 */
public class NamedChannelCredentialsProvider implements ChannelCredentialsProvider {

	private final SslBundles bundles;

	private final GrpcClientProperties properties;

	public NamedChannelCredentialsProvider(SslBundles bundles, GrpcClientProperties properties) {
		this.bundles = bundles;
		this.properties = properties;
	}

	@Override
	public ChannelCredentials getChannelCredentials(String path) {
		ChannelConfig channel = this.properties.getChannel(path);
		if (!channel.getSsl().isEnabled() && channel.getNegotiationType() == NegotiationType.PLAINTEXT) {
			return InsecureChannelCredentials.create();
		}
		if (channel.getSsl().isEnabled()) {
			String bundleName = channel.getSsl().getBundle();
			Assert.notNull(bundleName, "Bundle name must not be null when SSL is enabled");
			SslBundle bundle = this.bundles.getBundle(bundleName);
			TrustManagerFactory trustManagers = channel.isSecure() ? bundle.getManagers().getTrustManagerFactory()
					: InsecureTrustManagerFactory.INSTANCE;
			return TlsChannelCredentials.newBuilder()
				.keyManager(bundle.getManagers().getKeyManagerFactory().getKeyManagers())
				.trustManager(trustManagers.getTrustManagers())
				.build();
		}
		else {
			if (channel.isSecure()) {
				return TlsChannelCredentials.create();
			}
			else {
				return TlsChannelCredentials.newBuilder()
					.trustManager(InsecureTrustManagerFactory.INSTANCE.getTrustManagers())
					.build();
			}
		}
	}

}

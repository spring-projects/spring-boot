/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.client.autoconfigure;

import io.grpc.ChannelCredentials;
import io.grpc.InsecureChannelCredentials;
import io.grpc.TlsChannelCredentials;
import io.grpc.TlsChannelCredentials.Builder;

import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.Channel;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.Channel.Ssl;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.internal.InsecureTrustManagerFactory;

/**
 * {@link ChannelCredentialsProvider} backed by {@link GrpcClientProperties}.
 *
 * @param properties the client properties
 * @param bundles the SSL bundles
 * @author David Syer
 * @author Phillip Webb
 */
record PropertiesChannelCredentialsProvider(GrpcClientProperties properties,
		SslBundles bundles) implements ChannelCredentialsProvider {

	@Override
	public ChannelCredentials getChannelCredentials(String target) {
		Channel channel = this.properties.getChannel().get(target);
		channel = (channel != null) ? channel : this.properties.getChannel().get("default");
		if (channel == null || isInsecure(channel.getSsl())) {
			return InsecureChannelCredentials.create();
		}
		Builder builder = TlsChannelCredentials.newBuilder();
		if (channel.getSsl().getBundle() != null) {
			SslBundle bundle = this.bundles.getBundle(channel.getSsl().getBundle());
			builder.trustManager(bundle.getManagers().getTrustManagerFactory().getTrustManagers());
			builder.keyManager(bundle.getManagers().getKeyManagerFactory().getKeyManagers());
		}
		if (channel.isBypassCertificateValidation()) {
			builder.trustManager(InsecureTrustManagerFactory.INSTANCE.getTrustManagers());
		}
		return builder.build();
	}

	private boolean isInsecure(Ssl ssl) {
		return Boolean.FALSE.equals(ssl.getEnabled()) || (ssl.getBundle() == null && ssl.getEnabled() == null);
	}

}

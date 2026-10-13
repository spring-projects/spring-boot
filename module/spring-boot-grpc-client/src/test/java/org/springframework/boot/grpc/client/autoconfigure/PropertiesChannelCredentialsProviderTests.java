/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.client.autoconfigure;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import io.grpc.ChannelCredentials;
import io.grpc.InsecureChannelCredentials;
import io.grpc.TlsChannelCredentials;
import org.junit.jupiter.api.Test;

import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.Channel;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.grpc.client.ChannelCredentialsProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PropertiesChannelCredentialsProvider}.
 *
 * @author Phillip Webb
 */
class PropertiesChannelCredentialsProviderTests {

	private final TrustManager[] trustManagers = { mock() };

	private final KeyManager[] keyManagers = { mock() };

	@Test
	void getChannelCredentialsWhenTargetMatchesChannel() {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channelProperties = new Channel();
		channelProperties.getSsl().setEnabled(true);
		properties.getChannel().put("test", channelProperties);
		SslBundles sslBundles = new DefaultSslBundleRegistry();
		ChannelCredentialsProvider provider = new PropertiesChannelCredentialsProvider(properties, sslBundles);
		TlsChannelCredentials credentials = (TlsChannelCredentials) provider.getChannelCredentials("test");
		assertThat(credentials.getTrustManagers()).isNull();
		assertThat(credentials.getKeyManagers()).isNull();
	}

	@Test
	void getChannelCredentialsWhenTargetDoesNotMatchChannelAndHasDefault() {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channelProperties = new Channel();
		channelProperties.getSsl().setEnabled(true);
		properties.getChannel().put("default", channelProperties);
		SslBundles sslBundles = new DefaultSslBundleRegistry();
		ChannelCredentialsProvider provider = new PropertiesChannelCredentialsProvider(properties, sslBundles);
		TlsChannelCredentials credentials = (TlsChannelCredentials) provider.getChannelCredentials("test");
		assertThat(credentials.getTrustManagers()).isNull();
		assertThat(credentials.getKeyManagers()).isNull();
	}

	@Test
	void getChannelCredentialsWhenTargetDoesNotMatchChannelAndHasNoDefaultUsesInsecure() {
		GrpcClientProperties properties = new GrpcClientProperties();
		SslBundles sslBundles = new DefaultSslBundleRegistry();
		ChannelCredentialsProvider provider = new PropertiesChannelCredentialsProvider(properties, sslBundles);
		ChannelCredentials credentials = provider.getChannelCredentials("test");
		assertThat(credentials).isInstanceOf(InsecureChannelCredentials.class);
	}

	@Test
	void getChannelCredentialsWhenSslExplicitlyDisabled() {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channelProperties = new Channel();
		channelProperties.getSsl().setEnabled(false);
		properties.getChannel().put("test", channelProperties);
		SslBundles sslBundles = new DefaultSslBundleRegistry();
		ChannelCredentialsProvider provider = new PropertiesChannelCredentialsProvider(properties, sslBundles);
		ChannelCredentials credentials = provider.getChannelCredentials("test");
		assertThat(credentials).isInstanceOf(InsecureChannelCredentials.class);
	}

	@Test
	void getChannelCredentialsWhenSslExplicitlyEnabledAndNoBundle() {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channelProperties = new Channel();
		channelProperties.getSsl().setEnabled(true);
		properties.getChannel().put("test", channelProperties);
		SslBundles sslBundles = new DefaultSslBundleRegistry();
		ChannelCredentialsProvider provider = new PropertiesChannelCredentialsProvider(properties, sslBundles);
		TlsChannelCredentials credentials = (TlsChannelCredentials) provider.getChannelCredentials("test");
		assertThat(credentials.getTrustManagers()).isNull();
		assertThat(credentials.getKeyManagers()).isNull();
	}

	@Test
	void getChannelCredentialsWhenNoSslEnabledSetButHasBundle() {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channelProperties = new Channel();
		channelProperties.getSsl().setBundle("test");
		properties.getChannel().put("test", channelProperties);
		SslBundles sslBundles = new DefaultSslBundleRegistry("test", mockBundle());
		ChannelCredentialsProvider provider = new PropertiesChannelCredentialsProvider(properties, sslBundles);
		TlsChannelCredentials credentials = (TlsChannelCredentials) provider.getChannelCredentials("test");
		assertThat(credentials.getTrustManagers()).containsExactly(this.trustManagers);
		assertThat(credentials.getKeyManagers()).containsExactly(this.keyManagers);
	}

	@Test
	void getChannelCredentialsWhenNoSslEnabledSetAndNoBundle() {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channelProperties = new Channel();
		properties.getChannel().put("test", channelProperties);
		SslBundles sslBundles = new DefaultSslBundleRegistry();
		ChannelCredentialsProvider provider = new PropertiesChannelCredentialsProvider(properties, sslBundles);
		ChannelCredentials credentials = provider.getChannelCredentials("test");
		assertThat(credentials).isInstanceOf(InsecureChannelCredentials.class);
	}

	@Test
	void getChannelCredentialsWhenSslEnabledAndHasBundle() {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channelProperties = new Channel();
		channelProperties.getSsl().setEnabled(true);
		channelProperties.getSsl().setBundle("test");
		properties.getChannel().put("test", channelProperties);
		SslBundles sslBundles = new DefaultSslBundleRegistry("test", mockBundle());
		ChannelCredentialsProvider provider = new PropertiesChannelCredentialsProvider(properties, sslBundles);
		TlsChannelCredentials credentials = (TlsChannelCredentials) provider.getChannelCredentials("test");
		assertThat(credentials.getTrustManagers()).containsExactly(this.trustManagers);
		assertThat(credentials.getKeyManagers()).containsExactly(this.keyManagers);
	}

	@Test
	void getChannelCredentialsWhenBypassCertificateValidation() {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channelProperties = new Channel();
		channelProperties.setBypassCertificateValidation(true);
		channelProperties.getSsl().setBundle("test");
		properties.getChannel().put("test", channelProperties);
		SslBundles sslBundles = new DefaultSslBundleRegistry("test", mockBundle());
		ChannelCredentialsProvider provider = new PropertiesChannelCredentialsProvider(properties, sslBundles);
		TlsChannelCredentials credentials = (TlsChannelCredentials) provider.getChannelCredentials("test");
		TrustManager trustManager = credentials.getTrustManagers().get(0);
		assertThat(trustManager.getClass().getName()).contains("InsecureTrustManager");
		assertThat(((X509ExtendedTrustManager) trustManager).getAcceptedIssuers()).isEmpty();
		assertThat(credentials.getKeyManagers()).containsExactly(this.keyManagers);
	}

	private SslBundle mockBundle() {
		SslBundle bundle = mock();
		SslManagerBundle managerBundle = mock();
		TrustManagerFactory trustManagerFactory = mock();
		KeyManagerFactory keyManagerFactory = mock();
		given(bundle.getManagers()).willReturn(managerBundle);
		given(managerBundle.getTrustManagerFactory()).willReturn(trustManagerFactory);
		given(managerBundle.getKeyManagerFactory()).willReturn(keyManagerFactory);
		given(trustManagerFactory.getTrustManagers()).willReturn(this.trustManagers);
		given(keyManagerFactory.getKeyManagers()).willReturn(this.keyManagers);
		return bundle;
	}

}

/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;

/**
 * A {@link RabbitConnectionFactoryBean} that can be configured with custom SSL trust
 * material from an {@link SslBundle}.
 *
 * @author Scott Frederick
 */
class SslBundleRabbitConnectionFactoryBean extends RabbitConnectionFactoryBean {

	private @Nullable SslBundle sslBundle;

	private boolean enableHostnameVerification = true;

	@Override
	protected void setUpSSL() {
		if (this.sslBundle != null) {
			this.connectionFactory.useSslProtocol(this.sslBundle.createSslContext());
			applySslOptions(this.sslBundle.getOptions());
			if (this.enableHostnameVerification) {
				this.connectionFactory.enableHostnameVerification();
			}
		}
		else {
			super.setUpSSL();
		}
	}

	private void applySslOptions(SslOptions options) {
		this.connectionFactory
			.setSocketConfigurator(this.connectionFactory.getSocketConfigurator().andThen((socket) -> {
				if (socket instanceof SSLSocket sslSocket) {
					SSLParameters parameters = sslSocket.getSSLParameters();
					PropertyMapper map = PropertyMapper.get();
					map.from(options::getCiphers).to(parameters::setCipherSuites);
					map.from(options::getEnabledProtocols).to(parameters::setProtocols);
					sslSocket.setSSLParameters(parameters);
				}
			}));
	}

	void setSslBundle(@Nullable SslBundle sslBundle) {
		this.sslBundle = sslBundle;
	}

	@Deprecated(since = "4.2.0", forRemoval = true)
	@SuppressWarnings("removal")
	@Override
	public void setEnableHostnameVerification(boolean enable) {
		this.enableHostnameVerification = enable;
		super.setEnableHostnameVerification(enable);
	}

}

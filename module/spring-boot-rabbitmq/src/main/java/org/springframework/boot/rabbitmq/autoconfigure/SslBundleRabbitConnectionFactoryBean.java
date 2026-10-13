/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.boot.ssl.SslBundle;

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
			if (this.enableHostnameVerification) {
				this.connectionFactory.enableHostnameVerification();
			}
		}
		else {
			super.setUpSSL();
		}
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

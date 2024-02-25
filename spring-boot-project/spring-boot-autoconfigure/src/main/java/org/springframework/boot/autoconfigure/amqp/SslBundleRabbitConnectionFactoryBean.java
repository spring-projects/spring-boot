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

package org.springframework.boot.autoconfigure.amqp;

import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.boot.ssl.SslBundle;

/**
 * A {@link RabbitConnectionFactoryBean} that can be configured with custom SSL trust
 * material from an {@link SslBundle}.
 *
 * @author Scott Frederick
 */
class SslBundleRabbitConnectionFactoryBean extends RabbitConnectionFactoryBean {

	private SslBundle sslBundle;

	private boolean enableHostnameVerification;

	/**
     * Sets up SSL for the RabbitMQ connection factory.
     * If an SSL bundle is provided, it creates an SSL context using the bundle and sets it as the protocol for the connection factory.
     * If hostname verification is enabled, it enables hostname verification for the connection factory.
     * If no SSL bundle is provided, it calls the superclass method to set up SSL.
     */
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

	/**
     * Sets the SSL bundle for the RabbitMQ connection factory.
     * 
     * @param sslBundle the SSL bundle to set
     */
    void setSslBundle(SslBundle sslBundle) {
		this.sslBundle = sslBundle;
	}

	/**
     * Sets the flag to enable or disable hostname verification for SSL connections.
     * 
     * @param enable true to enable hostname verification, false to disable it
     */
    @Override
	public void setEnableHostnameVerification(boolean enable) {
		this.enableHostnameVerification = enable;
		super.setEnableHostnameVerification(enable);
	}

}

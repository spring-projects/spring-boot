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

package org.springframework.boot.kafka.autoconfigure;

import java.util.List;

import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Ssl;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapts {@link KafkaProperties} to {@link KafkaConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class PropertiesKafkaConnectionDetails implements KafkaConnectionDetails {

	private final KafkaProperties properties;

	private final SslBundles sslBundles;

	PropertiesKafkaConnectionDetails(KafkaProperties properties, SslBundles sslBundles) {
		this.properties = properties;
		this.sslBundles = sslBundles;
	}

	@Override
	public List<String> getBootstrapServers() {
		return this.properties.getBootstrapServers();
	}

	@Override
	public Configuration getConsumer() {
		List<String> servers = this.properties.getConsumer().getBootstrapServers();
		SslBundle sslBundle = getBundle(this.properties.getConsumer().getSsl());
		String protocol = this.properties.getConsumer().getSecurity().getProtocol();
		return Configuration.of((servers != null) ? servers : getBootstrapServers(),
				(sslBundle != null) ? sslBundle : getSslBundle(),
				(StringUtils.hasLength(protocol)) ? protocol : getSecurityProtocol());
	}

	@Override
	public Configuration getProducer() {
		List<String> servers = this.properties.getProducer().getBootstrapServers();
		SslBundle sslBundle = getBundle(this.properties.getProducer().getSsl());
		String protocol = this.properties.getProducer().getSecurity().getProtocol();
		return Configuration.of((servers != null) ? servers : getBootstrapServers(),
				(sslBundle != null) ? sslBundle : getSslBundle(),
				(StringUtils.hasLength(protocol)) ? protocol : getSecurityProtocol());
	}

	@Override
	public Configuration getStreams() {
		List<String> servers = this.properties.getStreams().getBootstrapServers();
		SslBundle sslBundle = getBundle(this.properties.getStreams().getSsl());
		String protocol = this.properties.getStreams().getSecurity().getProtocol();
		return Configuration.of((servers != null) ? servers : getBootstrapServers(),
				(sslBundle != null) ? sslBundle : getSslBundle(),
				(StringUtils.hasLength(protocol)) ? protocol : getSecurityProtocol());
	}

	@Override
	public Configuration getAdmin() {
		SslBundle sslBundle = getBundle(this.properties.getAdmin().getSsl());
		String protocol = this.properties.getAdmin().getSecurity().getProtocol();
		return Configuration.of(getBootstrapServers(), (sslBundle != null) ? sslBundle : getSslBundle(),
				(StringUtils.hasLength(protocol)) ? protocol : getSecurityProtocol());
	}

	@Override
	public SslBundle getSslBundle() {
		return getBundle(this.properties.getSsl());
	}

	@Override
	public String getSecurityProtocol() {
		return this.properties.getSecurity().getProtocol();
	}

	private SslBundle getBundle(Ssl ssl) {
		if (StringUtils.hasLength(ssl.getBundle())) {
			Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
			return this.sslBundles.getBundle(ssl.getBundle());
		}
		return null;
	}

}

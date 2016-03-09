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

package org.springframework.boot.autoconfigure.amqp;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RabbitProperties}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class RabbitPropertiesTests {

	private final RabbitProperties properties = new RabbitProperties();

	@Test
	public void addressesNotSet() {
		assertThat(this.properties.getHost()).isEqualTo("localhost");
		assertThat(this.properties.getPort()).isEqualTo(5672);
	}

	@Test
	public void addressesSingleValued() {
		this.properties.setAddresses("myhost:9999");
		assertThat(this.properties.getHost()).isEqualTo("myhost");
		assertThat(this.properties.getPort()).isEqualTo(9999);
	}

	@Test
	public void addressesDoubleValued() {
		this.properties.setAddresses("myhost:9999,otherhost:1111");
		assertThat(this.properties.getHost()).isNull();
		assertThat(this.properties.getPort()).isEqualTo(9999);
	}

	@Test
	public void addressesDoubleValuedWithCredentials() {
		this.properties.setAddresses("myhost:9999,root:password@otherhost:1111/host");
		assertThat(this.properties.getHost()).isNull();
		assertThat(this.properties.getPort()).isEqualTo(9999);
		assertThat(this.properties.getUsername()).isEqualTo("root");
		assertThat(this.properties.getVirtualHost()).isEqualTo("host");
	}

	@Test
	public void addressesDoubleValuedPreservesOrder() {
		this.properties.setAddresses("myhost:9999,ahost:1111/host");
		assertThat(this.properties.getHost()).isNull();
		assertThat(this.properties.getAddresses()).isEqualTo("myhost:9999,ahost:1111");
	}

	@Test
	public void addressesSingleValuedWithCredentials() {
		this.properties.setAddresses("amqp://root:password@otherhost:1111/host");
		assertThat(this.properties.getHost()).isEqualTo("otherhost");
		assertThat(this.properties.getPort()).isEqualTo(1111);
		assertThat(this.properties.getUsername()).isEqualTo("root");
		assertThat(this.properties.getVirtualHost()).isEqualTo("host");
	}

	@Test
	public void addressesSingleValuedWithCredentialsDefaultPort() {
		this.properties.setAddresses("amqp://root:password@lemur.cloudamqp.com/host");
		assertThat(this.properties.getHost()).isEqualTo("lemur.cloudamqp.com");
		assertThat(this.properties.getPort()).isEqualTo(5672);
		assertThat(this.properties.getUsername()).isEqualTo("root");
		assertThat(this.properties.getVirtualHost()).isEqualTo("host");
		assertThat(this.properties.getAddresses()).isEqualTo("lemur.cloudamqp.com:5672");
	}

	@Test
	public void addressWithTrailingSlash() {
		this.properties.setAddresses("amqp://root:password@otherhost:1111/");
		assertThat(this.properties.getHost()).isEqualTo("otherhost");
		assertThat(this.properties.getPort()).isEqualTo(1111);
		assertThat(this.properties.getUsername()).isEqualTo("root");
		assertThat(this.properties.getVirtualHost()).isEqualTo("/");
	}

	@Test
	public void testDefaultVirtualHost() {
		this.properties.setVirtualHost("/");
		assertThat(this.properties.getVirtualHost()).isEqualTo("/");
	}

	@Test
	public void testEmptyVirtualHost() {
		this.properties.setVirtualHost("");
		assertThat(this.properties.getVirtualHost()).isEqualTo("/");
	}

	@Test
	public void testCustomVirtualHost() {
		this.properties.setVirtualHost("myvHost");
		assertThat(this.properties.getVirtualHost()).isEqualTo("myvHost");
	}

	@Test
	public void testCustomFalsyVirtualHost() {
		this.properties.setVirtualHost("/myvHost");
		assertThat(this.properties.getVirtualHost()).isEqualTo("/myvHost");
	}

}

/*
 * Copyright 2012-2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link RabbitProperties}.
 *
 * @author Dave Syer
 */
public class RabbitPropertiesTests {

	private final RabbitProperties properties = new RabbitProperties();

	@Test
	public void addressesNotSet() {
		assertEquals("localhost", this.properties.getHost());
		assertEquals(5672, this.properties.getPort());
	}

	@Test
	public void addressesSingleValued() {
		this.properties.setAddresses("myhost:9999");
		assertEquals("myhost", this.properties.getHost());
		assertEquals(9999, this.properties.getPort());
	}

	@Test
	public void addressesDoubleValued() {
		this.properties.setAddresses("myhost:9999,otherhost:1111");
		assertNull(this.properties.getHost());
		assertEquals(9999, this.properties.getPort());
	}

	@Test
	public void addressesDoubleValuedWithCredentials() {
		this.properties.setAddresses("myhost:9999,root:password@otherhost:1111/host");
		assertNull(this.properties.getHost());
		assertEquals(9999, this.properties.getPort());
		assertEquals("root", this.properties.getUsername());
		assertEquals("host", this.properties.getVirtualHost());
	}

	@Test
	public void addressesDoubleValuedPreservesOrder() {
		this.properties.setAddresses("myhost:9999,ahost:1111/host");
		assertNull(this.properties.getHost());
		assertEquals("myhost:9999,ahost:1111", properties.getAddresses());
	}

	@Test
	public void addressesSingleValuedWithCredentials() {
		this.properties.setAddresses("amqp://root:password@otherhost:1111/host");
		assertEquals("otherhost", this.properties.getHost());
		assertEquals(1111, this.properties.getPort());
		assertEquals("root", this.properties.getUsername());
		assertEquals("host", this.properties.getVirtualHost());
	}

	@Test
	public void addressesSingleValuedWithCredentialsDefaultPort() {
		this.properties.setAddresses("amqp://root:password@lemur.cloudamqp.com/host");
		assertEquals("lemur.cloudamqp.com", this.properties.getHost());
		assertEquals(5672, this.properties.getPort());
		assertEquals("root", this.properties.getUsername());
		assertEquals("host", this.properties.getVirtualHost());
		assertEquals("lemur.cloudamqp.com:5672", this.properties.getAddresses());
	}

	@Test
	public void testDefaultVirtualHost() {
		this.properties.setVirtualHost("/");
		assertEquals("/", this.properties.getVirtualHost());
	}

	@Test
	public void testemptyVirtualHost() {
		this.properties.setVirtualHost("");
		assertEquals("/", this.properties.getVirtualHost());
	}

	@Test
	public void testCustomVirtualHost() {
		this.properties.setVirtualHost("myvHost");
		assertEquals("myvHost", this.properties.getVirtualHost());
	}

	@Test
	public void testCustomFalsyVirtualHost() {
		this.properties.setVirtualHost("/myvHost");
		assertEquals("/myvHost", this.properties.getVirtualHost());
	}

}

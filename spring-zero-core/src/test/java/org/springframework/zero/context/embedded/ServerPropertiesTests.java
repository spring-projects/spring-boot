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

package org.springframework.zero.context.embedded;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.zero.bind.RelaxedDataBinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests for {@link ServerProperties}.
 * 
 * @author Dave Syer
 */
public class ServerPropertiesTests {

	private ServerProperties properties = new ServerProperties();

	@Test
	public void testAddressBinding() throws Exception {
		RelaxedDataBinder binder = new RelaxedDataBinder(this.properties, "server");
		binder.bind(new MutablePropertyValues(Collections.singletonMap("server.address",
				"127.0.0.1")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertEquals(InetAddress.getByName("127.0.0.1"), this.properties.getAddress());
	}

	@Test
	public void testPortBinding() throws Exception {
		new RelaxedDataBinder(this.properties, "server").bind(new MutablePropertyValues(
				Collections.singletonMap("server.port", "9000")));
		assertEquals(9000, this.properties.getPort());
	}

	@Test
	public void testTomcatBinding() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.access_log_pattern", "%h %t '%r' %s %b");
		map.put("server.tomcat.protocol_header", "X-Forwarded-Protocol");
		map.put("server.tomcat.remote_ip_header", "Remote-Ip");
		new RelaxedDataBinder(this.properties, "server").bind(new MutablePropertyValues(
				map));
		assertEquals("%h %t '%r' %s %b", this.properties.getTomcat()
				.getAccessLogPattern());
		assertEquals("Remote-Ip", this.properties.getTomcat().getRemoteIpHeader());
		assertEquals("X-Forwarded-Protocol", this.properties.getTomcat()
				.getProtocolHeader());
	}

	// FIXME test customize

}

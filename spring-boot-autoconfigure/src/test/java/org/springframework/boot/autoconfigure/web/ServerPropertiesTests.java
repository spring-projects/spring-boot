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

package org.springframework.boot.autoconfigure.web;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.Valve;
import org.apache.catalina.valves.RemoteIpValve;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ServerProperties}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
public class ServerPropertiesTests {

	private final ServerProperties properties = new ServerProperties();

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
		assertEquals(9000, this.properties.getPort().intValue());
	}

	@Test
	public void testServletPathAsMapping() throws Exception {
		RelaxedDataBinder binder = new RelaxedDataBinder(this.properties, "server");
		binder.bind(new MutablePropertyValues(Collections.singletonMap(
				"server.servletPath", "/foo/*")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertEquals("/foo/*", this.properties.getServletMapping());
		assertEquals("/foo", this.properties.getServletPrefix());
	}

	@Test
	public void testServletPathAsPrefix() throws Exception {
		RelaxedDataBinder binder = new RelaxedDataBinder(this.properties, "server");
		binder.bind(new MutablePropertyValues(Collections.singletonMap(
				"server.servletPath", "/foo")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertEquals("/foo/*", this.properties.getServletMapping());
		assertEquals("/foo", this.properties.getServletPrefix());
	}

	@Test
	public void testTomcatBinding() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.access_log_pattern", "%h %t '%r' %s %b");
		map.put("server.tomcat.protocol_header", "X-Forwarded-Protocol");
		map.put("server.tomcat.remote_ip_header", "Remote-Ip");
		map.put("server.tomcat.internal_proxies", "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
		bindProperties(map);
		assertEquals("%h %t '%r' %s %b", this.properties.getTomcat()
				.getAccessLogPattern());
		assertEquals("Remote-Ip", this.properties.getTomcat().getRemoteIpHeader());
		assertEquals("X-Forwarded-Protocol", this.properties.getTomcat()
				.getProtocolHeader());
		assertEquals("10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", this.properties.getTomcat()
				.getInternalProxies());
	}

	@Test
	public void testCustomizeTomcat() throws Exception {
		ConfigurableEmbeddedServletContainer factory = mock(ConfigurableEmbeddedServletContainer.class);
		this.properties.customize(factory);
		verify(factory, never()).setContextPath("");
	}

	@Test
	public void testCustomizeTomcatPort() throws Exception {
		ConfigurableEmbeddedServletContainer factory = mock(ConfigurableEmbeddedServletContainer.class);
		this.properties.setPort(8080);
		this.properties.customize(factory);
		verify(factory).setPort(8080);
	}

	@Test
	public void testCustomizeUriEncoding() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.uriEncoding", "US-ASCII");
		bindProperties(map);
		assertEquals("US-ASCII", this.properties.getTomcat().getUriEncoding());
	}

	@Test
	public void testCustomizeTomcatHeaderSize() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.maxHttpHeaderSize", "9999");
		bindProperties(map);
		assertEquals(9999, this.properties.getTomcat().getMaxHttpHeaderSize());
	}

	@Test
	public void disableTomcatRemoteIpValve() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.remote_ip_header", "");
		map.put("server.tomcat.protocol_header", "");
		bindProperties(map);

		TomcatEmbeddedServletContainerFactory container = new TomcatEmbeddedServletContainerFactory();
		this.properties.customize(container);

		assertEquals(0, container.getValves().size());
	}

	@Test
	public void defaultTomcatRemoteIpValve() throws Exception {
		TomcatEmbeddedServletContainerFactory container = new TomcatEmbeddedServletContainerFactory();
		this.properties.customize(container);

		assertEquals(1, container.getValves().size());
		Valve valve = container.getValves().iterator().next();
		assertThat(valve, instanceOf(RemoteIpValve.class));
		RemoteIpValve remoteIpValve = (RemoteIpValve) valve;
		assertEquals("x-forwarded-proto", remoteIpValve.getProtocolHeader());
		assertEquals("x-forwarded-for", remoteIpValve.getRemoteIpHeader());

		String expectedInternalProxies = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 10/8
				+ "192\\.168\\.\\d{1,3}\\.\\d{1,3}|" // 192.168/16
				+ "169\\.254\\.\\d{1,3}\\.\\d{1,3}|" // 169.254/16
				+ "127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"; // 127/8

		assertEquals(expectedInternalProxies, remoteIpValve.getInternalProxies());
	}

	@Test
	public void customTomcatRemoteIpValve() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.remote_ip_header", "x-my-remote-ip-header");
		map.put("server.tomcat.protocol_header", "x-my-protocol-header");
		map.put("server.tomcat.internal_proxies", "192.168.0.1");
		bindProperties(map);

		TomcatEmbeddedServletContainerFactory container = new TomcatEmbeddedServletContainerFactory();
		this.properties.customize(container);

		assertEquals(1, container.getValves().size());
		Valve valve = container.getValves().iterator().next();
		assertThat(valve, instanceOf(RemoteIpValve.class));
		RemoteIpValve remoteIpValve = (RemoteIpValve) valve;
		assertEquals("x-my-protocol-header", remoteIpValve.getProtocolHeader());
		assertEquals("x-my-remote-ip-header", remoteIpValve.getRemoteIpHeader());
		assertEquals("192.168.0.1", remoteIpValve.getInternalProxies());
	}

	private void bindProperties(Map<String, String> map) {
		new RelaxedDataBinder(this.properties, "server").bind(new MutablePropertyValues(
				map));
	}

}

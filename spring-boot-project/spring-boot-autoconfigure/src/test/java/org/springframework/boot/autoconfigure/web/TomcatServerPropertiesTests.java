/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.web.server.tomcat.TomcatServerProperties;
import org.springframework.boot.autoconfigure.web.server.tomcat.TomcatServerProperties.Accesslog;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatServerProperties}.
 *
 * @author Andy Wilkinson
 */
class TomcatServerPropertiesTests {

	private final TomcatServerProperties properties = new TomcatServerProperties();

	@Test
	void testTomcatBinding() {
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.accesslog.conditionIf", "foo");
		map.put("server.tomcat.accesslog.conditionUnless", "bar");
		map.put("server.tomcat.accesslog.pattern", "%h %t '%r' %s %b");
		map.put("server.tomcat.accesslog.prefix", "foo");
		map.put("server.tomcat.accesslog.suffix", "-bar.log");
		map.put("server.tomcat.accesslog.encoding", "UTF-8");
		map.put("server.tomcat.accesslog.locale", "en-AU");
		map.put("server.tomcat.accesslog.checkExists", "true");
		map.put("server.tomcat.accesslog.rotate", "false");
		map.put("server.tomcat.accesslog.rename-on-rotate", "true");
		map.put("server.tomcat.accesslog.ipv6Canonical", "true");
		map.put("server.tomcat.accesslog.request-attributes-enabled", "true");
		map.put("server.tomcat.remoteip.protocol-header", "X-Forwarded-Protocol");
		map.put("server.tomcat.remoteip.remote-ip-header", "Remote-Ip");
		map.put("server.tomcat.remoteip.internal-proxies", "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
		map.put("server.tomcat.remoteip.trusted-proxies", "proxy1|proxy2|proxy3");
		map.put("server.tomcat.reject-illegal-header", "false");
		map.put("server.tomcat.background-processor-delay", "10");
		map.put("server.tomcat.relaxed-path-chars", "|,<");
		map.put("server.tomcat.relaxed-query-chars", "^  ,  | ");
		map.put("server.tomcat.use-relative-redirects", "true");
		bind(map);
		Accesslog accesslog = this.properties.getAccesslog();
		assertThat(accesslog.getConditionIf()).isEqualTo("foo");
		assertThat(accesslog.getConditionUnless()).isEqualTo("bar");
		assertThat(accesslog.getPattern()).isEqualTo("%h %t '%r' %s %b");
		assertThat(accesslog.getPrefix()).isEqualTo("foo");
		assertThat(accesslog.getSuffix()).isEqualTo("-bar.log");
		assertThat(accesslog.getEncoding()).isEqualTo("UTF-8");
		assertThat(accesslog.getLocale()).isEqualTo("en-AU");
		assertThat(accesslog.isCheckExists()).isTrue();
		assertThat(accesslog.isRotate()).isFalse();
		assertThat(accesslog.isRenameOnRotate()).isTrue();
		assertThat(accesslog.isIpv6Canonical()).isTrue();
		assertThat(accesslog.isRequestAttributesEnabled()).isTrue();
		assertThat(this.properties.getRemoteip().getRemoteIpHeader()).isEqualTo("Remote-Ip");
		assertThat(this.properties.getRemoteip().getProtocolHeader()).isEqualTo("X-Forwarded-Protocol");
		assertThat(this.properties.getRemoteip().getInternalProxies()).isEqualTo("10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
		assertThat(this.properties.getRemoteip().getTrustedProxies()).isEqualTo("proxy1|proxy2|proxy3");
		assertThat(this.properties.getBackgroundProcessorDelay()).hasSeconds(10);
		assertThat(this.properties.getRelaxedPathChars()).containsExactly('|', '<');
		assertThat(this.properties.getRelaxedQueryChars()).containsExactly('^', '|');
		assertThat(this.properties.isUseRelativeRedirects()).isTrue();
	}

	@Test
	void testCustomizeTomcatUriEncoding() {
		bind("server.tomcat.uri-encoding", "US-ASCII");
		assertThat(this.properties.getUriEncoding()).isEqualTo(StandardCharsets.US_ASCII);
	}

	@Test
	void testCustomizeTomcatMaxThreads() {
		bind("server.tomcat.threads.max", "10");
		assertThat(this.properties.getThreads().getMax()).isEqualTo(10);
	}

	@Test
	void testCustomizeTomcatKeepAliveTimeout() {
		bind("server.tomcat.keep-alive-timeout", "30s");
		assertThat(this.properties.getKeepAliveTimeout()).hasSeconds(30);
	}

	@Test
	void testCustomizeTomcatKeepAliveTimeoutWithInfinite() {
		bind("server.tomcat.keep-alive-timeout", "-1");
		assertThat(this.properties.getKeepAliveTimeout()).hasMillis(-1);
	}

	@Test
	void testCustomizeTomcatMaxKeepAliveRequests() {
		bind("server.tomcat.max-keep-alive-requests", "200");
		assertThat(this.properties.getMaxKeepAliveRequests()).isEqualTo(200);
	}

	@Test
	void testCustomizeTomcatMaxKeepAliveRequestsWithInfinite() {
		bind("server.tomcat.max-keep-alive-requests", "-1");
		assertThat(this.properties.getMaxKeepAliveRequests()).isEqualTo(-1);
	}

	@Test
	void testCustomizeTomcatMaxParameterCount() {
		bind("server.tomcat.max-parameter-count", "100");
		assertThat(this.properties.getMaxParameterCount()).isEqualTo(100);
	}

	@Test
	void testCustomizeTomcatMinSpareThreads() {
		bind("server.tomcat.threads.min-spare", "10");
		assertThat(this.properties.getThreads().getMinSpare()).isEqualTo(10);
	}

	@Test
	void tomcatAcceptCountMatchesProtocolDefault() throws Exception {
		assertThat(this.properties.getAcceptCount()).isEqualTo(getDefaultProtocol().getAcceptCount());
	}

	@Test
	void tomcatProcessorCacheMatchesProtocolDefault() throws Exception {
		assertThat(this.properties.getProcessorCache()).isEqualTo(getDefaultProtocol().getProcessorCache());
	}

	@Test
	void tomcatMaxConnectionsMatchesProtocolDefault() throws Exception {
		assertThat(this.properties.getMaxConnections()).isEqualTo(getDefaultProtocol().getMaxConnections());
	}

	@Test
	void tomcatMaxThreadsMatchesProtocolDefault() throws Exception {
		assertThat(this.properties.getThreads().getMax()).isEqualTo(getDefaultProtocol().getMaxThreads());
	}

	@Test
	void tomcatMinSpareThreadsMatchesProtocolDefault() throws Exception {
		assertThat(this.properties.getThreads().getMinSpare()).isEqualTo(getDefaultProtocol().getMinSpareThreads());
	}

	@Test
	void tomcatMaxHttpPostSizeMatchesConnectorDefault() {
		assertThat(this.properties.getMaxHttpFormPostSize().toBytes())
			.isEqualTo(getDefaultConnector().getMaxPostSize());
	}

	@Test
	void tomcatMaxParameterCountMatchesConnectorDefault() {
		assertThat(this.properties.getMaxParameterCount()).isEqualTo(getDefaultConnector().getMaxParameterCount());
	}

	@Test
	void tomcatBackgroundProcessorDelayMatchesEngineDefault() {
		assertThat(this.properties.getBackgroundProcessorDelay())
			.hasSeconds((new StandardEngine().getBackgroundProcessorDelay()));
	}

	@Test
	void tomcatMaxHttpFormPostSizeMatchesConnectorDefault() {
		assertThat(this.properties.getMaxHttpFormPostSize().toBytes())
			.isEqualTo(getDefaultConnector().getMaxPostSize());
	}

	@Test
	void tomcatUriEncodingMatchesConnectorDefault() {
		assertThat(this.properties.getUriEncoding().name()).isEqualTo(getDefaultConnector().getURIEncoding());
	}

	@Test
	void tomcatRedirectContextRootMatchesDefault() {
		assertThat(this.properties.getRedirectContextRoot())
			.isEqualTo(new StandardContext().getMapperContextRootRedirectEnabled());
	}

	@Test
	void tomcatAccessLogRenameOnRotateMatchesDefault() {
		assertThat(this.properties.getAccesslog().isRenameOnRotate())
			.isEqualTo(new AccessLogValve().isRenameOnRotate());
	}

	@Test
	void tomcatAccessLogRequestAttributesEnabledMatchesDefault() {
		assertThat(this.properties.getAccesslog().isRequestAttributesEnabled())
			.isEqualTo(new AccessLogValve().getRequestAttributesEnabled());
	}

	@Test
	void tomcatInternalProxiesMatchesDefault() {
		assertThat(this.properties.getRemoteip().getInternalProxies())
			.isEqualTo(new RemoteIpValve().getInternalProxies());
	}

	@Test
	void tomcatUseRelativeRedirectsDefaultsToFalse() {
		assertThat(this.properties.isUseRelativeRedirects()).isFalse();
	}

	@Test
	void tomcatMaxKeepAliveRequestsDefault() throws Exception {
		AbstractEndpoint<?, ?> endpoint = (AbstractEndpoint<?, ?>) ReflectionTestUtils.getField(getDefaultProtocol(),
				"endpoint");
		int defaultMaxKeepAliveRequests = (int) ReflectionTestUtils.getField(endpoint, "maxKeepAliveRequests");
		assertThat(this.properties.getMaxKeepAliveRequests()).isEqualTo(defaultMaxKeepAliveRequests);
	}

	private void bind(String name, String value) {
		bind(Collections.singletonMap(name, value));
	}

	private void bind(Map<String, String> map) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("server.tomcat", Bindable.ofInstance(this.properties));
	}

	private Connector getDefaultConnector() {
		return new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
	}

	private AbstractProtocol<?> getDefaultProtocol() throws Exception {
		return (AbstractProtocol<?>) Class.forName(TomcatServletWebServerFactory.DEFAULT_PROTOCOL)
			.getDeclaredConstructor()
			.newInstance();
	}

}

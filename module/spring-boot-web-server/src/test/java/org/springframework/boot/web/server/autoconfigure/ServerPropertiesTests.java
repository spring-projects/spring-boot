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

package org.springframework.boot.web.server.autoconfigure;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;

import org.apache.coyote.http11.Http11Nio2Protocol;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.MimeMappings.Mapping;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServerProperties}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Quinten De Swaef
 * @author Venil Noronha
 * @author Andrew McGhie
 * @author HaiTao Zhang
 * @author Rafiullah Hamedy
 * @author Chris Bono
 * @author Parviz Rozikov
 * @author Lasse Wulff
 * @author Moritz Halbritter
 */
class ServerPropertiesTests {

	private final ServerProperties properties = new ServerProperties();

	@Test
	void testAddressBinding() throws Exception {
		bind("server.address", "127.0.0.1");
		assertThat(this.properties.getAddress()).isEqualTo(InetAddress.getByName("127.0.0.1"));
	}

	@Test
	void testPortBinding() {
		bind("server.port", "9000");
		Integer port = this.properties.getPort();
		assertThat(port).isNotNull();
		assertThat(port.intValue()).isEqualTo(9000);
	}

	@Test
	void testServerHeaderDefault() {
		assertThat(this.properties.getServerHeader()).isNull();
	}

	@Test
	void testServerHeader() {
		bind("server.server-header", "Custom Server");
		assertThat(this.properties.getServerHeader()).isEqualTo("Custom Server");
	}

	@Test
	void testTrailingSlashOfContextPathIsRemoved() {
		bind("server.servlet.context-path", "/foo/");
		assertThat(this.properties.getServlet().getContextPath()).isEqualTo("/foo");
	}

	@Test
	void testSlashOfContextPathIsDefaultValue() {
		bind("server.servlet.context-path", "/");
		assertThat(this.properties.getServlet().getContextPath()).isEmpty();
	}

	@Test
	void testContextPathWithLeadingWhitespace() {
		bind("server.servlet.context-path", " /assets");
		assertThat(this.properties.getServlet().getContextPath()).isEqualTo("/assets");
	}

	@Test
	void testContextPathWithTrailingWhitespace() {
		bind("server.servlet.context-path", "/assets/copy/ ");
		assertThat(this.properties.getServlet().getContextPath()).isEqualTo("/assets/copy");
	}

	@Test
	void testContextPathWithLeadingAndTrailingWhitespace() {
		bind("server.servlet.context-path", " /assets ");
		assertThat(this.properties.getServlet().getContextPath()).isEqualTo("/assets");
	}

	@Test
	void testContextPathWithLeadingAndTrailingWhitespaceAndContextWithSpace() {
		bind("server.servlet.context-path", "  /assets /copy/    ");
		assertThat(this.properties.getServlet().getContextPath()).isEqualTo("/assets /copy");
	}

	@Test
	void testDefaultMimeMapping() {
		assertThat(this.properties.getMimeMappings()).isEmpty();
	}

	@Test
	void testCustomizedMimeMapping() {
		MimeMappings expectedMappings = new MimeMappings();
		expectedMappings.add("mjs", "text/javascript");
		bind("server.mime-mappings.mjs", "text/javascript");
		assertThat(this.properties.getMimeMappings())
			.containsExactly(expectedMappings.getAll().toArray(new Mapping[0]));
	}

	@Test
	void testCustomizeMaxHttpRequestHeaderSize() {
		bind("server.max-http-request-header-size", "1MB");
		assertThat(this.properties.getMaxHttpRequestHeaderSize()).isEqualTo(DataSize.ofMegabytes(1));
	}

	@Test
	void testCustomizeMaxHttpRequestHeaderSizeUseBytesByDefault() {
		bind("server.max-http-request-header-size", "1024");
		assertThat(this.properties.getMaxHttpRequestHeaderSize()).isEqualTo(DataSize.ofKilobytes(1));
	}

	@Test
	void defaultMaxHttpRequestHeaderSizeMatchesTomcatsDefault() {
		assertThat(this.properties.getMaxHttpRequestHeaderSize().toBytes())
			.isEqualTo(new Http11Nio2Protocol().getMaxHttpRequestHeaderSize());
	}

	@Test
	void additionalCompressionMimeTypesAreAddedToDefaults() {
		bind("server.compression.additional-mime-types", "application/zip");
		assertThat(this.properties.getCompression().getAllMimeTypes()).contains(new Compression().getMimeTypes());
		assertThat(this.properties.getCompression().getAllMimeTypes()).contains("application/zip");
	}

	private void bind(String name, String value) {
		bind(Collections.singletonMap(name, value));
	}

	private void bind(Map<String, String> map) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("server", Bindable.ofInstance(this.properties));
	}

}

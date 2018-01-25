/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.embedded.tomcat;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.core.env.Environment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link TomcatCustomizer}
 *
 * @author Yulin Qin
 */

public class TomcatCustomizerTest {
	@Mock
	private ServerProperties serverProperties;

	@Mock
	private Environment environment;

	@Mock
	private ConfigurableTomcatWebServerFactory factory;
	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private org.springframework.boot.autoconfigure.web.ServerProperties.Tomcat tomcatProperties;

	@Before
	public void setUp() throws Exception {
		given(this.serverProperties.getTomcat()).willReturn(this.tomcatProperties);
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void shouldNotSetBaseDirWhenItIsNull() throws Exception {
		given(this.tomcatProperties.getBasedir()).willReturn(null);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).setBaseDirectory(any());
	}

	@Test
	public void shouldSetBaseDirWhenItIsNotNull() throws Exception {
		File mockFile = mock(File.class);
		given(this.tomcatProperties.getBasedir()).willReturn(mockFile);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setBaseDirectory(mockFile);
	}

	@Test
	public void shouldNotSetBackgroundProcessorDelayWhenItIsNull() throws Exception {
		given(this.tomcatProperties.getBackgroundProcessorDelay()).willReturn(null);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).setBackgroundProcessorDelay(any());
	}

	@Test
	public void shouldSetBackgroundProcessorDelayWhenItIsNotNull() throws Exception {
		given(this.tomcatProperties.getBackgroundProcessorDelay()).willReturn(Duration.ZERO);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setBackgroundProcessorDelay(any());
	}

	@Test
	public void shouldCustomizeRemoteIpValveWhenProtocolHeaderHasText() throws Exception {
		given(this.tomcatProperties.getProtocolHeader()).willReturn("AnyProtocolHeader");
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addEngineValves(any());
	}

	@Test
	public void shouldCustomizeRemoteIpValveWhenRemoteIpHeaderHasText() throws Exception {
		given(this.tomcatProperties.getRemoteIpHeader()).willReturn("AnyRemoteHeader");
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addEngineValves(any());
	}

	@Test
	public void shouldCustomizeRemoteIpValveWhenUseForwardHeadersisTrue() throws Exception {
		given(this.serverProperties.isUseForwardHeaders()).willReturn(Boolean.TRUE);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addEngineValves(any());
	}

	@Test
	public void shouldSetMaxThreadsWhenItIsGreaterThanZero() throws Exception {
		given(this.tomcatProperties.getMaxThreads()).willReturn(1);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldNotSetMaxThreadsWhenItEqualsToZero() throws Exception {
		given(this.tomcatProperties.getMaxThreads()).willReturn(0);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldNotSetMaxThreadsWhenItIsLessThanZero() throws Exception {
		given(this.tomcatProperties.getMaxThreads()).willReturn(-1);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldCustomizeMinThreadsWhenItIsGreaterThanZero() throws Exception {
		given(this.tomcatProperties.getMinSpareThreads()).willReturn(1);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldNotCustomizeMinThreadsWhenItEqualsToZero() throws Exception {
		given(this.tomcatProperties.getMinSpareThreads()).willReturn(0);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldNotCustomizeMinThreadsWhenItIsLessThanZero() throws Exception {
		given(this.tomcatProperties.getMinSpareThreads()).willReturn(-1);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldCustomizeMaxHttpHeaderSizeWhenItIsGreaterThanZeroInServerProperties() throws Exception {
		given(this.serverProperties.getMaxHttpHeaderSize()).willReturn(1);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldCustomizeMaxHttpHeaderSizeWhenItIsGreaterThanZeroInTomcatProperties() throws Exception {
		given(this.serverProperties.getMaxHttpHeaderSize()).willReturn(-1);
		given(this.tomcatProperties.getMaxHttpHeaderSize()).willReturn(1);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldNotCustomizeMaxHttpHeaderSizeWhenItIsLessThanZeroInServerProperties() throws Exception {
		given(this.serverProperties.getMaxHttpHeaderSize()).willReturn(-1);
		given(this.tomcatProperties.getMaxHttpHeaderSize()).willReturn(-1);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldCustomizeMaxHttpPostSizeWhenItDoesNotEqualToZero() throws Exception {
		given(this.tomcatProperties.getMaxHttpPostSize()).willReturn(-1);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldNotCustomizeMaxHttpPostSizeWhenItEqualsToZero() throws Exception {
		given(this.tomcatProperties.getMaxHttpPostSize()).willReturn(0);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldCustomizeAccessLogWhenItIsEnabled() throws Exception {
		given(this.tomcatProperties.getAccesslog().isEnabled()).willReturn(Boolean.TRUE);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldNotCustomizeAccessLogWhenItIsDisabled() throws Exception {
		given(this.tomcatProperties.getAccesslog().isEnabled()).willReturn(Boolean.FALSE);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldSetUriEncodingWhenItIsNotNull() throws Exception {
		given(this.tomcatProperties.getUriEncoding()).willReturn(Charset.defaultCharset());
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setUriEncoding(any());
	}

	@Test
	public void shouldNotSetUriEncodingWhenItIsNull() throws Exception {
		given(this.tomcatProperties.getUriEncoding()).willReturn(null);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).setUriEncoding(any());
	}

	@Test
	public void shouldCustomizeConnectionTimeoutWhenItIsNotNull() throws Exception {
		given(this.serverProperties.getConnectionTimeout()).willReturn(Duration.ZERO);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldNotCustomizeConnectionTimeoutWhenItIsNull() throws Exception {
		given(this.serverProperties.getConnectionTimeout()).willReturn(null);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldCustomizeMaxConnectionsWhenItIsGreaterThanZero() throws Exception {
		given(this.tomcatProperties.getMaxConnections()).willReturn(1);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldNotCustomizeMaxConnectionsWhenItEqualsToZero() throws Exception {
		given(this.tomcatProperties.getMaxConnections()).willReturn(0);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldNotCustomizeMaxConnectionsWhenItIsLessThanZero() throws Exception {
		given(this.tomcatProperties.getMaxConnections()).willReturn(-1);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldCustomizeAcceptCountWhenItIsGreaterThanZero() throws Exception {
		given(this.tomcatProperties.getAcceptCount()).willReturn(1);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addConnectorCustomizers(any());

	}

	@Test
	public void shouldNotCustomizeAcceptCountWhenItEqualsToZero() throws Exception {
		given(this.tomcatProperties.getAcceptCount()).willReturn(0);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldNotCustomizeAcceptCountWhenItIsLessThanZero() throws Exception {
		given(this.tomcatProperties.getAcceptCount()).willReturn(-1);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldCustomizeStaticResourcesWhenCacheTtlNotNull() throws Exception {
		given(this.tomcatProperties.getResource().getCacheTtl()).willReturn(Duration.ZERO);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addConnectorCustomizers(any());
	}

	@Test
	public void shouldNotCustomizeStaticResourcesWhenCacheTtlNull() throws Exception {
		given(this.tomcatProperties.getResource().getCacheTtl()).willReturn(null);
		TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addConnectorCustomizers(any());
	}
}

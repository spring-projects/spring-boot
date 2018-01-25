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

package org.springframework.boot.autoconfigure.web.embedded.jetty;


import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.jetty.ConfigurableJettyWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.core.env.Environment;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JettyCustomizer}
 *
 * @author Yulin Qin
 */

public class JettyCustomizerTest {
	@Mock
	private ServerProperties serverProperties;
	@Mock
	private Environment environment;
	@Mock
	private ConfigurableJettyWebServerFactory factory;
	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ServerProperties.Jetty jettyProperties;


	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		given(this.serverProperties.getJetty()).willReturn(this.jettyProperties);
		given(this.jettyProperties.getAccesslog().isEnabled()).willReturn(Boolean.FALSE);
	}

	@Test
	public void shouldSetUseForwardHeadersToFalseWhenItIsFalse() throws Exception {
		given(this.serverProperties.isUseForwardHeaders()).willReturn(Boolean.FALSE);
		JettyCustomizer.customizeJetty(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setUseForwardHeaders(Boolean.FALSE);
	}

	@Test
	public void shouldSetUseForwardHeadersToTrueWhenItIsTrue() throws Exception {
		given(this.serverProperties.isUseForwardHeaders()).willReturn(Boolean.TRUE);
		JettyCustomizer.customizeJetty(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setUseForwardHeaders(Boolean.TRUE);
	}

	@Test
	public void shouldSetUseForwardHeadersToTrueWhenItIsNullButIsCloudActive() throws Exception {
		given(this.serverProperties.isUseForwardHeaders()).willReturn(null);
		given(this.environment.containsProperty(any())).willReturn(true);
		JettyCustomizer.customizeJetty(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setUseForwardHeaders(Boolean.TRUE);
	}

	@Test
	public void shouldSetAcceptorsWhenItIsNotNull() throws Exception {
		given(this.jettyProperties.getAcceptors()).willReturn(1);
		JettyCustomizer.customizeJetty(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setAcceptors(1);
	}

	@Test
	public void shouldNotSetAcceptorsWhenItIsNull() throws Exception {
		given(this.jettyProperties.getAcceptors()).willReturn(null);
		JettyCustomizer.customizeJetty(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).setAcceptors(anyInt());
	}

	@Test
	public void shouldSetSelectorsWhenItIsNotNull() throws Exception {
		given(this.jettyProperties.getSelectors()).willReturn(1);
		JettyCustomizer.customizeJetty(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setSelectors(1);
	}

	@Test
	public void shouldNotSetSelectorsWhenItIsNull() throws Exception {
		given(this.jettyProperties.getSelectors()).willReturn(null);
		JettyCustomizer.customizeJetty(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).setSelectors(anyInt());
	}


	@Test
	public void shouldCustomizeMaxHttpHeaderSizeWhenItIsLargerThanZero() throws Exception {
		given(this.serverProperties.getMaxHttpHeaderSize()).willReturn(1);
		JettyCustomizer.customizeJetty(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addServerCustomizers(any(JettyServerCustomizer.class));
	}

	@Test
	public void shouldNotCustomizeMaxHttpHeaderSizeWhenItEqualsToZero() throws Exception {
		given(this.serverProperties.getMaxHttpHeaderSize()).willReturn(0);
		JettyCustomizer.customizeJetty(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addServerCustomizers(any(JettyServerCustomizer.class));
	}

	@Test
	public void shouldNotCustomizeMaxHttpHeaderSizeWhenItIsLessThanZero() throws Exception {
		given(this.serverProperties.getMaxHttpHeaderSize()).willReturn(-1);
		JettyCustomizer.customizeJetty(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addServerCustomizers(any(JettyServerCustomizer.class));
	}

	@Test
	public void shouldCustomizeMaxHttpPostSizeWhenItIsLargerThanZero() throws Exception {
		given(this.jettyProperties.getMaxHttpPostSize()).willReturn(1);
		JettyCustomizer.customizeJetty(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addServerCustomizers(any(JettyServerCustomizer.class));
	}

	@Test
	public void shouldNotCustomizeMaxHttpPostSizeWhenItEqualsToZero() throws Exception {
		given(this.jettyProperties.getMaxHttpPostSize()).willReturn(0);
		JettyCustomizer.customizeJetty(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addServerCustomizers(any(JettyServerCustomizer.class));
	}

	@Test
	public void shouldNotCustomizeMaxHttpPostSizeWhenItIsLessThanZero() throws Exception {
		given(this.jettyProperties.getMaxHttpPostSize()).willReturn(-1);
		JettyCustomizer.customizeJetty(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addServerCustomizers(any(JettyServerCustomizer.class));
	}

	@Test
	public void shouldCustomizeConnectionTimeoutWhenItIsNotNull() throws Exception {
		given(this.serverProperties.getConnectionTimeout()).willReturn(Duration.ZERO);
		JettyCustomizer.customizeJetty(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addServerCustomizers(any(JettyServerCustomizer.class));
	}

	@Test
	public void shouldNotCustomizeConnectionTimeoutWhenItIsNull() throws Exception {
		given(this.serverProperties.getConnectionTimeout()).willReturn(null);
		JettyCustomizer.customizeJetty(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addServerCustomizers(any(JettyServerCustomizer.class));
	}

}

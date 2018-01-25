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

package org.springframework.boot.autoconfigure.web.embedded.undertow;

import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.undertow.ConfigurableUndertowWebServerFactory;
import org.springframework.core.env.Environment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link UndertowCustomizer}
 *
 * @author Yulin Qin
 */

public class UndertowCustomizerTest {

	@Mock
	private ServerProperties serverProperties;

	@Mock
	private Environment environment;

	@Mock
	private ConfigurableUndertowWebServerFactory factory;

	@Mock
	private org.springframework.boot.autoconfigure.web.ServerProperties.Undertow undertowProperties;
	@Mock
	private org.springframework.boot.autoconfigure.web.ServerProperties.Undertow.Accesslog accesslogProperties;


	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		given(this.serverProperties.getUndertow()).willReturn(this.undertowProperties);
		given(this.undertowProperties.getAccesslog()).willReturn(this.accesslogProperties);
	}

	@Test
	public void shouldNotSetBufferSizeWhileItIsNull() throws Exception {
		given(this.undertowProperties.getBufferSize()).willReturn(null);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).setBufferSize(anyInt());
	}

	@Test
	public void shouldSetBufferSizeWhileItIsNotNull() throws Exception {
		given(this.undertowProperties.getBufferSize()).willReturn(1);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setBufferSize(anyInt());
	}

	@Test
	public void shouldNotSetIoThreadWhileItIsNull() throws Exception {
		given(this.undertowProperties.getIoThreads()).willReturn(null);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).setIoThreads(any());
	}

	@Test
	public void shouldSetIoThreadWhileItIsNotNull() throws Exception {
		given(this.undertowProperties.getIoThreads()).willReturn(1);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setIoThreads(any());
	}

	@Test
	public void shouldNotSetWorkerThreadsWhileItIsNull() throws Exception {
		given(this.undertowProperties.getWorkerThreads()).willReturn(null);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).setWorkerThreads(any());
	}

	@Test
	public void shouldSetWorkerThreadsWhileItIsNotNull() throws Exception {
		given(this.undertowProperties.getWorkerThreads()).willReturn(1);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setWorkerThreads(any());
	}

	@Test
	public void shouldNotSetDirectBuffersWhileItIsNull() throws Exception {
		given(this.undertowProperties.getDirectBuffers()).willReturn(null);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).setUseDirectBuffers(anyBoolean());
	}

	@Test
	public void shouldSetDirectBuffersWhileItIsNotNull() throws Exception {
		given(this.undertowProperties.getDirectBuffers()).willReturn(Boolean.TRUE);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setUseDirectBuffers(anyBoolean());
	}

	@Test
	public void shouldNotSetAccesslogWhileItIsNull() throws Exception {
		given(this.accesslogProperties.getEnabled()).willReturn(null);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).setAccessLogEnabled(any());
	}

	@Test
	public void shouldSetAccesslogWhileItIsNotNull() throws Exception {
		given(this.accesslogProperties.getEnabled()).willReturn(Boolean.FALSE);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setAccessLogEnabled(any());
	}

	@Test
	public void shouldAlwaysSetAccessLogDirectory() throws Exception {
		given(this.accesslogProperties.getDir()).willReturn(null);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setAccessLogDirectory(any());
	}

	@Test
	public void shouldAlwaysSetAccessLogPattern() throws Exception {
		given(this.accesslogProperties.getPattern()).willReturn(null);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setAccessLogPattern(any());
	}

	@Test
	public void shouldAlwaysSetAccessLogPrefix() throws Exception {
		given(this.accesslogProperties.getPrefix()).willReturn(null);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setAccessLogPrefix(any());
	}

	@Test
	public void shouldAlwaysSetAccessLogSuffix() throws Exception {
		given(this.accesslogProperties.getSuffix()).willReturn(null);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setAccessLogSuffix(any());
	}

	@Test
	public void shouldAlwaysSetAccessLogRotate() throws Exception {
		given(this.accesslogProperties.isRotate()).willReturn(false);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setAccessLogRotate(any());
	}

	@Test
	public void shouldSetUserForwardHeaderWhenItIsTrue() throws Exception {
		given(this.serverProperties.isUseForwardHeaders()).willReturn(true);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setUseForwardHeaders(Boolean.TRUE);
	}

	@Test
	public void shouldSetUserForwardHeaderWhenItIsNUll() throws Exception {
		given(this.serverProperties.isUseForwardHeaders()).willReturn(null);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).setUseForwardHeaders(any());
	}

	@Test
	public void shouldCustomizeMaxHttpHeaderSizeWhenItIsGreaterThanZero() throws Exception {
		given(this.serverProperties.getMaxHttpHeaderSize()).willReturn(1);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addBuilderCustomizers(any());
	}

	@Test
	public void shouldNotCustomizeMaxHttpHeaderSizeWhenItIsEqualZero() throws Exception {
		given(this.serverProperties.getMaxHttpHeaderSize()).willReturn(0);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addBuilderCustomizers(any());
	}

	@Test
	public void shouldNotCustomizeMaxHttpHeaderSizeWhenItIsLessThanZero() throws Exception {
		given(this.serverProperties.getMaxHttpHeaderSize()).willReturn(-1);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addBuilderCustomizers(any());
	}

	@Test
	public void shouldCustomizeMaxHttpPostSizeWhenItIsGreaterThanZero() throws Exception {
		given(this.undertowProperties.getMaxHttpPostSize()).willReturn(Long.valueOf(1));
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addBuilderCustomizers(any());
	}

	@Test
	public void shouldNotCustomizeMaxHttpPostSizeWhenItIsEqualZero() throws Exception {
		given(this.undertowProperties.getMaxHttpPostSize()).willReturn(Long.valueOf(0));
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addBuilderCustomizers(any());
	}

	@Test
	public void shouldNotCustomizeMaxHttpPostSizeWhenItIsLessThanZero() throws Exception {
		given(this.undertowProperties.getMaxHttpPostSize()).willReturn(Long.valueOf(-1));
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addBuilderCustomizers(any());
	}

	@Test
	public void shouldCustomizeConnectionTimeoutWhenItIsNotNull() throws Exception {
		given(this.serverProperties.getConnectionTimeout()).willReturn(Duration.ZERO);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addBuilderCustomizers(any());
	}

	@Test
	public void shouldNotCustomizeConnectionTimeoutWhenItIsNull() throws Exception {
		given(this.serverProperties.getConnectionTimeout()).willReturn(null);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(0)).addBuilderCustomizers(any());
	}

	@Test
	public void shouldAlwaysCallAddDeploymentInfoCustomizers() throws Exception {
		given(this.undertowProperties.isEagerFilterInit()).willReturn(null);
		UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment, this.factory);
		verify(this.factory, times(1)).addDeploymentInfoCustomizers(any());
	}

}

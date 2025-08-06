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

package org.springframework.boot.undertow;

import java.io.File;
import java.util.Collection;

import io.undertow.Undertow.Builder;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.web.server.ConfigurableWebServerFactory;

/**
 * {@link ConfigurableWebServerFactory} for Undertow-specific features.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
public interface ConfigurableUndertowWebServerFactory extends ConfigurableWebServerFactory {

	/**
	 * Set {@link UndertowBuilderCustomizer}s that should be applied to the Undertow
	 * {@link Builder}. Calling this method will replace any existing customizers.
	 * @param customizers the customizers to set
	 * @since 4.0.0
	 */
	void setBuilderCustomizers(Collection<? extends UndertowBuilderCustomizer> customizers);

	/**
	 * Add {@link UndertowBuilderCustomizer}s that should be used to customize the
	 * Undertow {@link Builder}.
	 * @param customizers the customizers to add
	 */
	void addBuilderCustomizers(UndertowBuilderCustomizer... customizers);

	/**
	 * Set the buffer size.
	 * @param bufferSize buffer size
	 */
	void setBufferSize(@Nullable Integer bufferSize);

	/**
	 * Set the number of IO Threads.
	 * @param ioThreads number of IO Threads
	 */
	void setIoThreads(@Nullable Integer ioThreads);

	/**
	 * Set the number of Worker Threads.
	 * @param workerThreads number of Worker Threads
	 */
	void setWorkerThreads(@Nullable Integer workerThreads);

	/**
	 * Set whether direct buffers should be used.
	 * @param useDirectBuffers whether direct buffers should be used
	 */
	void setUseDirectBuffers(@Nullable Boolean useDirectBuffers);

	/**
	 * Set the access log directory.
	 * @param accessLogDirectory access log directory
	 */
	void setAccessLogDirectory(@Nullable File accessLogDirectory);

	/**
	 * Set the access log pattern.
	 * @param accessLogPattern access log pattern
	 */
	void setAccessLogPattern(@Nullable String accessLogPattern);

	/**
	 * Set the access log prefix.
	 * @param accessLogPrefix log prefix
	 */
	void setAccessLogPrefix(@Nullable String accessLogPrefix);

	/**
	 * Set the access log suffix.
	 * @param accessLogSuffix access log suffix
	 */
	void setAccessLogSuffix(@Nullable String accessLogSuffix);

	/**
	 * Set whether access logs are enabled.
	 * @param accessLogEnabled whether access logs are enabled
	 */
	void setAccessLogEnabled(boolean accessLogEnabled);

	/**
	 * Set whether access logs rotation is enabled.
	 * @param accessLogRotate whether access logs rotation is enabled
	 */
	void setAccessLogRotate(boolean accessLogRotate);

	/**
	 * Set if x-forward-* headers should be processed.
	 * @param useForwardHeaders if x-forward headers should be used
	 */
	void setUseForwardHeaders(boolean useForwardHeaders);

}

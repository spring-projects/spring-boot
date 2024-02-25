/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.context;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.convert.DurationUnit;

/**
 * Configuration properties for Message Source.
 *
 * @author Stephane Nicoll
 * @author Kedar Joshi
 * @since 2.0.0
 */
public class MessageSourceProperties {

	/**
	 * Comma-separated list of basenames (essentially a fully-qualified classpath
	 * location), each following the ResourceBundle convention with relaxed support for
	 * slash based locations. If it doesn't contain a package qualifier (such as
	 * "org.mypackage"), it will be resolved from the classpath root.
	 */
	private String basename = "messages";

	/**
	 * Message bundles encoding.
	 */
	private Charset encoding = StandardCharsets.UTF_8;

	/**
	 * Loaded resource bundle files cache duration. When not set, bundles are cached
	 * forever. If a duration suffix is not specified, seconds will be used.
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration cacheDuration;

	/**
	 * Whether to fall back to the system Locale if no files for a specific Locale have
	 * been found. if this is turned off, the only fallback will be the default file (e.g.
	 * "messages.properties" for basename "messages").
	 */
	private boolean fallbackToSystemLocale = true;

	/**
	 * Whether to always apply the MessageFormat rules, parsing even messages without
	 * arguments.
	 */
	private boolean alwaysUseMessageFormat = false;

	/**
	 * Whether to use the message code as the default message instead of throwing a
	 * "NoSuchMessageException". Recommended during development only.
	 */
	private boolean useCodeAsDefaultMessage = false;

	/**
	 * Returns the basename of the MessageSourceProperties.
	 * @return the basename of the MessageSourceProperties
	 */
	public String getBasename() {
		return this.basename;
	}

	/**
	 * Sets the basename for the message source properties.
	 * @param basename the basename to set
	 */
	public void setBasename(String basename) {
		this.basename = basename;
	}

	/**
	 * Returns the encoding used by the MessageSourceProperties.
	 * @return the encoding used by the MessageSourceProperties
	 */
	public Charset getEncoding() {
		return this.encoding;
	}

	/**
	 * Sets the encoding for the message source.
	 * @param encoding the encoding to be set
	 */
	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	/**
	 * Returns the cache duration for the message source.
	 * @return the cache duration for the message source
	 */
	public Duration getCacheDuration() {
		return this.cacheDuration;
	}

	/**
	 * Sets the cache duration for the message source.
	 * @param cacheDuration the cache duration to be set
	 */
	public void setCacheDuration(Duration cacheDuration) {
		this.cacheDuration = cacheDuration;
	}

	/**
	 * Returns a boolean value indicating whether the fallback to the system locale is
	 * enabled.
	 * @return {@code true} if fallback to the system locale is enabled, {@code false}
	 * otherwise
	 */
	public boolean isFallbackToSystemLocale() {
		return this.fallbackToSystemLocale;
	}

	/**
	 * Sets the flag indicating whether to fallback to the system locale if a message is
	 * not found in the specified locale.
	 * @param fallbackToSystemLocale true to enable fallback to system locale, false
	 * otherwise
	 */
	public void setFallbackToSystemLocale(boolean fallbackToSystemLocale) {
		this.fallbackToSystemLocale = fallbackToSystemLocale;
	}

	/**
	 * Returns a boolean value indicating whether the message format should always be
	 * used.
	 * @return true if the message format should always be used, false otherwise
	 */
	public boolean isAlwaysUseMessageFormat() {
		return this.alwaysUseMessageFormat;
	}

	/**
	 * Sets the flag indicating whether to always use message format for resolving
	 * messages.
	 * @param alwaysUseMessageFormat the flag indicating whether to always use message
	 * format
	 */
	public void setAlwaysUseMessageFormat(boolean alwaysUseMessageFormat) {
		this.alwaysUseMessageFormat = alwaysUseMessageFormat;
	}

	/**
	 * Returns a boolean value indicating whether to use the code as the default message.
	 * @return true if the code should be used as the default message, false otherwise
	 */
	public boolean isUseCodeAsDefaultMessage() {
		return this.useCodeAsDefaultMessage;
	}

	/**
	 * Sets the flag indicating whether to use the code as the default message.
	 * @param useCodeAsDefaultMessage the flag indicating whether to use the code as the
	 * default message
	 */
	public void setUseCodeAsDefaultMessage(boolean useCodeAsDefaultMessage) {
		this.useCodeAsDefaultMessage = useCodeAsDefaultMessage;
	}

}

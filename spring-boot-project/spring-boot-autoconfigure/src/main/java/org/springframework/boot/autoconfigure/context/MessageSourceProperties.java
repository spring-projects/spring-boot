/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.context;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Configuration properties for Message Source.
 *
 * @author Stephane Nicoll
 * @author Kedar Joshi
 * @since 2.0.0
 */
public class MessageSourceProperties {

	/**
	 * Comma-separated list of basenames, each following the ResourceBundle convention.
	 * Essentially a fully-qualified classpath location. If it doesn't contain a package
	 * qualifier (such as "org.mypackage"), it will be resolved from the classpath root.
	 */
	private String basename = "messages";

	/**
	 * Message bundles encoding.
	 */
	private Charset encoding = StandardCharsets.UTF_8;

	/**
	 * Loaded resource bundle files cache expiration, in seconds. When set to -1, bundles
	 * are cached forever.
	 */
	private int cacheSeconds = -1;

	/**
	 * Set whether to fall back to the system Locale if no files for a specific Locale
	 * have been found. if this is turned off, the only fallback will be the default file
	 * (e.g. "messages.properties" for basename "messages").
	 */
	private boolean fallbackToSystemLocale = true;

	/**
	 * Set whether to always apply the MessageFormat rules, parsing even messages without
	 * arguments.
	 */
	private boolean alwaysUseMessageFormat = false;

	/**
	 * Set whether to use the message code as default message instead of throwing a
	 * "NoSuchMessageException". Recommended during development only.
	 */
	private boolean useCodeAsDefaultMessage = false;

	public String getBasename() {
		return this.basename;
	}

	public void setBasename(String basename) {
		this.basename = basename;
	}

	public Charset getEncoding() {
		return this.encoding;
	}

	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	public int getCacheSeconds() {
		return this.cacheSeconds;
	}

	public void setCacheSeconds(int cacheSeconds) {
		this.cacheSeconds = cacheSeconds;
	}

	public boolean isFallbackToSystemLocale() {
		return this.fallbackToSystemLocale;
	}

	public void setFallbackToSystemLocale(boolean fallbackToSystemLocale) {
		this.fallbackToSystemLocale = fallbackToSystemLocale;
	}

	public boolean isAlwaysUseMessageFormat() {
		return this.alwaysUseMessageFormat;
	}

	public void setAlwaysUseMessageFormat(boolean alwaysUseMessageFormat) {
		this.alwaysUseMessageFormat = alwaysUseMessageFormat;
	}

	public boolean isUseCodeAsDefaultMessage() {
		return this.useCodeAsDefaultMessage;
	}

	public void setUseCodeAsDefaultMessage(final boolean useCodeAsDefaultMessage) {
		this.useCodeAsDefaultMessage = useCodeAsDefaultMessage;
	}

}

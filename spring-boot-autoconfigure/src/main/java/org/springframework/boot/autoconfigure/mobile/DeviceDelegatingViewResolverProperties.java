/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.mobile;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for device view resolver.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
@ConfigurationProperties("spring.mobile.devicedelegatingviewresolver")
public class DeviceDelegatingViewResolverProperties {

	/**
	 * Enable support for fallback resolution.
	 */
	private boolean enableFallback;

	/**
	 * Prefix that gets prepended to view names for normal devices.
	 */
	private String normalPrefix = "";

	/**
	 * Suffix that gets appended to view names for normal devices.
	 */
	private String normalSuffix = "";

	/**
	 * Prefix that gets prepended to view names for mobile devices.
	 */
	private String mobilePrefix = "mobile/";

	/**
	 * Suffix that gets appended to view names for mobile devices.
	 */
	private String mobileSuffix = "";

	/**
	 * Prefix that gets prepended to view names for tablet devices.
	 */
	private String tabletPrefix = "tablet/";

	/**
	 * Suffix that gets appended to view names for tablet devices.
	 */
	private String tabletSuffix = "";

	public void setEnableFallback(boolean enableFallback) {
		this.enableFallback = enableFallback;
	}

	public boolean isEnableFallback() {
		return enableFallback;
	}

	public String getNormalPrefix() {
		return this.normalPrefix;
	}

	public void setNormalPrefix(String normalPrefix) {
		this.normalPrefix = normalPrefix;
	}

	public String getNormalSuffix() {
		return this.normalSuffix;
	}

	public void setNormalSuffix(String normalSuffix) {
		this.normalSuffix = normalSuffix;
	}

	public String getMobilePrefix() {
		return this.mobilePrefix;
	}

	public void setMobilePrefix(String mobilePrefix) {
		this.mobilePrefix = mobilePrefix;
	}

	public String getMobileSuffix() {
		return this.mobileSuffix;
	}

	public void setMobileSuffix(String mobileSuffix) {
		this.mobileSuffix = mobileSuffix;
	}

	public String getTabletPrefix() {
		return this.tabletPrefix;
	}

	public void setTabletPrefix(String tabletPrefix) {
		this.tabletPrefix = tabletPrefix;
	}

	public String getTabletSuffix() {
		return this.tabletSuffix;
	}

	public void setTabletSuffix(String tabletSuffix) {
		this.tabletSuffix = tabletSuffix;
	}

}

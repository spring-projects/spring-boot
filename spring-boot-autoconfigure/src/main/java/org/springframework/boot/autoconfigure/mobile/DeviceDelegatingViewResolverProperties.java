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

package org.springframework.boot.autoconfigure.mobile;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.mobile.device.view.LiteDeviceDelegatingViewResolver;

/**
 * {@link ConfigurationProperties properties} for device view resolver.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
@ConfigurationProperties("spring.mobile.devicedelegatingviewresolver")
public class DeviceDelegatingViewResolverProperties {

	private String normalPrefix = "";

	private String normalSuffix = "";

	private String mobilePrefix = "mobile/";

	private String mobileSuffix = "";

	private String tabletPrefix = "tablet/";

	private String tabletSuffix = "";

	public String getNormalPrefix() {
		return normalPrefix;
	}

	public void setNormalPrefix(String normalPrefix) {
		this.normalPrefix = normalPrefix;
	}

	public String getNormalSuffix() {
		return normalSuffix;
	}

	public void setNormalSuffix(String normalSuffix) {
		this.normalSuffix = normalSuffix;
	}

	public String getMobilePrefix() {
		return mobilePrefix;
	}

	public void setMobilePrefix(String mobilePrefix) {
		this.mobilePrefix = mobilePrefix;
	}

	public String getMobileSuffix() {
		return mobileSuffix;
	}

	public void setMobileSuffix(String mobileSuffix) {
		this.mobileSuffix = mobileSuffix;
	}

	public String getTabletPrefix() {
		return tabletPrefix;
	}

	public void setTabletPrefix(String tabletPrefix) {
		this.tabletPrefix = tabletPrefix;
	}

	public String getTabletSuffix() {
		return tabletSuffix;
	}

	public void setTabletSuffix(String tabletSuffix) {
		this.tabletSuffix = tabletSuffix;
	}

	public void apply(LiteDeviceDelegatingViewResolver resolver) {
		resolver.setNormalPrefix(getNormalPrefix());
		resolver.setNormalSuffix(getNormalSuffix());
		resolver.setMobilePrefix(getMobilePrefix());
		resolver.setMobileSuffix(getMobileSuffix());
		resolver.setTabletPrefix(getTabletPrefix());
		resolver.setTabletSuffix(getTabletSuffix());
	}

}

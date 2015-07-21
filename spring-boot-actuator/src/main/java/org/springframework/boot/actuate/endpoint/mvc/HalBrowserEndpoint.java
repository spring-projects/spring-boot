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

package org.springframework.boot.actuate.endpoint.mvc;

import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * {@link MvcEndpoint} to support the Spring Data HAL browser.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@ConfigurationProperties("endpoints.hal")
public class HalBrowserEndpoint extends WebMvcConfigurerAdapter implements MvcEndpoint {

	private static final String HAL_BROWSER_VERSION = "b7669f1-1";

	private static final String HAL_BROWSER_LOCATION = "classpath:/META-INF/resources/webjars/hal-browser/"
			+ HAL_BROWSER_VERSION + "/";

	private String path;

	private ManagementServerProperties management;

	private boolean sensitive = false;

	public HalBrowserEndpoint(ManagementServerProperties management, String defaultPath) {
		this.management = management;
		this.path = defaultPath;
	}

	@RequestMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
	public String browse() {
		return "forward:" + this.management.getContextPath() + this.path
				+ "/browser.html";
	}

	@RequestMapping(value = "", produces = MediaType.TEXT_HTML_VALUE)
	public String redirect() {
		return "redirect:" + this.management.getContextPath() + this.path + "/#"
				+ this.management.getContextPath();
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// Make sure the root path is not cached so the browser comes back for the JSON
		registry.addResourceHandler(this.management.getContextPath() + this.path + "/")
				.addResourceLocations(HAL_BROWSER_LOCATION).setCachePeriod(0);
		registry.addResourceHandler(this.management.getContextPath() + this.path + "/**")
				.addResourceLocations(HAL_BROWSER_LOCATION);
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	public void setSensitive(boolean sensitive) {
		this.sensitive = sensitive;
	}

	@Override
	public boolean isSensitive() {
		return this.sensitive;
	}

	@Override
	public Class<? extends Endpoint<?>> getEndpointType() {
		return null;
	}

}

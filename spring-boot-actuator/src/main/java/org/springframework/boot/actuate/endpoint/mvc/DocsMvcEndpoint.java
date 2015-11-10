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

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.EndpointProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * {@link MvcEndpoint} to expose actuator documentation.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@ConfigurationProperties("endpoints.docs")
public class DocsMvcEndpoint extends WebMvcConfigurerAdapter
		implements MvcEndpoint, EnvironmentAware {

	private static final String DOCS_LOCATION = "classpath:/META-INF/resources/spring-boot-actuator/docs/";

	private Environment environment;

	private String path = "/docs";

	/**
	 * Enable the endpoint.
	 */
	private boolean enabled = true;

	/**
	 * Mark if the endpoint exposes sensitive information.
	 */
	private Boolean sensitive;

	private final ManagementServletContext managementServletContext;

	private Curies curies = new Curies();

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public Curies getCuries() {
		return this.curies;
	}

	public DocsMvcEndpoint(ManagementServletContext managementServletContext) {
		this.managementServletContext = managementServletContext;
	}

	@RequestMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
	public String browse() {
		return "forward:" + this.managementServletContext.getContextPath() + this.path
				+ "/index.html";
	}

	@RequestMapping(value = "", produces = MediaType.TEXT_HTML_VALUE)
	public String redirect() {
		return "redirect:" + this.managementServletContext.getContextPath() + this.path
				+ "/";
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler(
				this.managementServletContext.getContextPath() + this.path + "/**")
				.addResourceLocations(DOCS_LOCATION);
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean isSensitive() {
		return EndpointProperties.isSensitive(this.environment, this.sensitive, false);
	}

	public void setSensitive(Boolean sensitive) {
		this.sensitive = sensitive;
	}

	@Override
	public Class<? extends Endpoint<?>> getEndpointType() {
		return null;
	}

	/**
	 * Properties of the default CurieProvider (used for adding docs links). If enabled,
	 * all unqualified rels will pick up a prefix and a curie template pointing to the
	 * docs endpoint.
	 */
	public static class Curies {

		/**
		 * Enable the curie generation.
		 */
		private boolean enabled = false;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}

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

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.EndpointProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * {@link MvcEndpoint} to expose HAL-formatted JSON.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@ConfigurationProperties("endpoints.actuator")
public class HalJsonMvcEndpoint extends WebMvcConfigurerAdapter
		implements MvcEndpoint, EnvironmentAware {

	private Environment environment;

	/**
	 * Endpoint URL path.
	 */
	@NotNull
	@Pattern(regexp = "^$|/[^/]*", message = "Path must be empty or start with /")
	private String path;

	/**
	 * Enable the endpoint.
	 */
	private boolean enabled = true;

	/**
	 * Mark if the endpoint exposes sensitive information.
	 */
	private Boolean sensitive;

	private final ManagementServletContext managementServletContext;

	public HalJsonMvcEndpoint(ManagementServletContext managementServletContext) {
		this.managementServletContext = managementServletContext;
		this.path = getDefaultPath(managementServletContext);
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	private String getDefaultPath(ManagementServletContext managementServletContext) {
		if (StringUtils.hasText(managementServletContext.getContextPath())) {
			return "";
		}
		return "/actuator";
	}

	@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResourceSupport links() {
		return new ResourceSupport();
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

	protected final ManagementServletContext getManagementServletContext() {
		return this.managementServletContext;
	}

}

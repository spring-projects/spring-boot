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
 * {@link MvcEndpoint} to expose actuator documentation.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@ConfigurationProperties("endpoints.docs")
public class ActuatorDocsEndpoint extends WebMvcConfigurerAdapter implements MvcEndpoint {

	private static final String DOCS_LOCATION = "classpath:/META-INF/resources/spring-boot-actuator/docs/";

	private String path = "/docs";

	private boolean sensitive;

	private ManagementServerProperties management;

	private Curies curies = new Curies();

	public Curies getCuries() {
		return this.curies;
	}

	public ActuatorDocsEndpoint(ManagementServerProperties management) {
		this.management = management;
	}

	@RequestMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
	public String browse() {
		return "forward:" + this.management.getContextPath() + this.path + "/index.html";
	}

	@RequestMapping(value = "", produces = MediaType.TEXT_HTML_VALUE)
	public String redirect() {
		return "redirect:" + this.management.getContextPath() + this.path + "/";
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler(this.management.getContextPath() + this.path + "/**")
				.addResourceLocations(DOCS_LOCATION);
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

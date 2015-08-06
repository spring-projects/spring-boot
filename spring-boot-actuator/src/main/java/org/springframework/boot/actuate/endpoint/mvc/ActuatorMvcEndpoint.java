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

import java.io.IOException;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

/**
 * {@link MvcEndpoint} for the actuator. Uses content negotiation to provide access to the
 * HAL browser (when on the classpath), and to HAL-formatted JSON.
 *
 * @author Dave Syer
 * @author Phil Webb
 * @author Andy Wilkinson
 */
@ConfigurationProperties("endpoints.actuator")
public class ActuatorMvcEndpoint extends WebMvcConfigurerAdapter implements MvcEndpoint,
		ResourceLoaderAware {

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private static HalBrowserLocation[] HAL_BROWSER_RESOURCE_LOCATIONS = {
			new HalBrowserLocation("classpath:/META-INF/spring-data-rest/hal-browser/",
					"index.html"),
			new HalBrowserLocation(
					"classpath:/META-INF/resources/webjars/hal-browser/b7669f1-1/",
					"browser.html") };

	/**
	 * Endpoint URL path.
	 */
	@NotNull
	@Pattern(regexp = "^$|/[^/]*", message = "Path must be empty or start with /")
	private String path;

	/**
	 * Enable security on the endpoint.
	 */
	private boolean sensitive = false;

	/**
	 * Enable the endpoint.
	 */
	private boolean enabled = true;

	private final ManagementServerProperties management;

	private HalBrowserLocation location;

	public ActuatorMvcEndpoint(ManagementServerProperties management) {
		this.management = management;
		if (StringUtils.hasText(management.getContextPath())) {
			this.path = "";
		}
		else {
			this.path = "/actuator";
		}
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.location = getHalBrowserLocation(resourceLoader);
	}

	@RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
	public String browse(HttpServletRequest request) {
		if (this.location == null) {
			throw new HalBrowserUnavailableException();
		}
		String contextPath = this.management.getContextPath()
				+ (this.path.endsWith("/") ? this.path : this.path + "/");
		if (request.getRequestURI().endsWith("/")) {
			return "forward:" + contextPath + this.location.getHtmlFile();
		}
		return "redirect:" + contextPath;
	}

	@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResourceSupport links() {
		return new ResourceSupport();
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// Make sure the root path is not cached so the browser comes back for the JSON
		// and add a transformer to set the initial link
		if (this.location != null) {
			String start = this.management.getContextPath() + this.path;
			registry.addResourceHandler(start + "/", start + "/**")
					.addResourceLocations(this.location.getResourceLocation())
					.setCachePeriod(0).resourceChain(true)
					.addTransformer(new InitialUrlTransformer());
		}
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public boolean isSensitive() {
		return this.sensitive;
	}

	public void setSensitive(boolean sensitive) {
		this.sensitive = sensitive;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public Class<? extends Endpoint<?>> getEndpointType() {
		return null;
	}

	public static HalBrowserLocation getHalBrowserLocation(ResourceLoader resourceLoader) {
		for (HalBrowserLocation candidate : HAL_BROWSER_RESOURCE_LOCATIONS) {
			try {
				Resource resource = resourceLoader.getResource(candidate.toString());
				if (resource != null && resource.exists()) {
					return candidate;
				}
			}
			catch (Exception ex) {
			}
		}
		return null;
	}

	/**
	 * {@link ResourceTransformer} to change the initial link location.
	 */
	private class InitialUrlTransformer implements ResourceTransformer {

		@Override
		public Resource transform(HttpServletRequest request, Resource resource,
				ResourceTransformerChain transformerChain) throws IOException {
			resource = transformerChain.transform(request, resource);
			if (resource.getFilename().equalsIgnoreCase(
					ActuatorMvcEndpoint.this.location.getHtmlFile())) {
				return replaceInitialLink(resource);
			}
			return resource;
		}

		private Resource replaceInitialLink(Resource resource) throws IOException {
			byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
			String content = new String(bytes, DEFAULT_CHARSET);
			String initialLink = ActuatorMvcEndpoint.this.management.getContextPath()
					+ getPath();
			content = content.replace("entryPoint: '/'", "entryPoint: '" + initialLink
					+ "'");
			return new TransformedResource(resource, content.getBytes(DEFAULT_CHARSET));
		}

	}

	public static class HalBrowserLocation {

		private final String resourceLocation;

		private final String htmlFile;

		public HalBrowserLocation(String resourceLocation, String html) {
			this.resourceLocation = resourceLocation;
			this.htmlFile = html;
		}

		public String getResourceLocation() {
			return this.resourceLocation;
		}

		public String getHtmlFile() {
			return this.htmlFile;
		}

		@Override
		public String toString() {
			return this.resourceLocation + this.htmlFile;
		}

	}

	@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
	private static class HalBrowserUnavailableException extends RuntimeException {

	}

}

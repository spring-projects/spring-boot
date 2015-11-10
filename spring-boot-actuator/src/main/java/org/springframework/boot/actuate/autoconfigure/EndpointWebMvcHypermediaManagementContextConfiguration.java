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

package org.springframework.boot.actuate.autoconfigure;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.mvc.DocsMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HalBrowserMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HalJsonMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HypermediaDisabled;
import org.springframework.boot.actuate.endpoint.mvc.ManagementServletContext;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.ResourceLoader;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.hal.CurieProvider;
import org.springframework.hateoas.hal.DefaultCurieProvider;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.TypeUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

/**
 * Configuration for hypermedia in HTTP endpoints.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@ManagementContextConfiguration
@ConditionalOnClass(Link.class)
@ConditionalOnWebApplication
@ConditionalOnBean(HttpMessageConverters.class)
@ConditionalOnProperty(value = "endpoints.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ResourceProperties.class)
public class EndpointWebMvcHypermediaManagementContextConfiguration {

	@Bean
	public ManagementServletContext managementServletContext(
			final ManagementServerProperties properties) {
		return new ManagementServletContext() {

			@Override
			public String getContextPath() {
				return properties.getContextPath();
			}

		};
	}

	@ConditionalOnProperty(prefix = "endpoints.actuator", name = "enabled", matchIfMissing = true)
	@Bean
	public HalJsonMvcEndpoint halJsonMvcEndpoint(
			ManagementServletContext managementServletContext,
			ResourceProperties resources, ResourceLoader resourceLoader) {
		if (HalBrowserMvcEndpoint.getHalBrowserLocation(resourceLoader) != null) {
			return new HalBrowserMvcEndpoint(managementServletContext);
		}
		return new HalJsonMvcEndpoint(managementServletContext);
	}

	@Bean
	@ConditionalOnProperty(prefix = "endpoints.docs", name = "enabled", matchIfMissing = true)
	@ConditionalOnResource(resources = "classpath:/META-INF/resources/spring-boot-actuator/docs/index.html")
	public DocsMvcEndpoint docsMvcEndpoint(
			ManagementServletContext managementServletContext) {
		return new DocsMvcEndpoint(managementServletContext);
	}

	@Bean
	@ConditionalOnBean(DocsMvcEndpoint.class)
	@ConditionalOnMissingBean(CurieProvider.class)
	@ConditionalOnProperty(prefix = "endpoints.docs.curies", name = "enabled", matchIfMissing = false)
	public DefaultCurieProvider curieProvider(ServerProperties server,
			ManagementServerProperties management, DocsMvcEndpoint endpoint) {
		String path = management.getContextPath() + endpoint.getPath()
				+ "/#spring_boot_actuator__{rel}";
		if (server.getPort() == management.getPort() && management.getPort() != null
				&& management.getPort() != 0) {
			path = server.getPath(path);
		}
		return new DefaultCurieProvider("boot", new UriTemplate(path));
	}

	/**
	 * Controller advice that adds links to the actuator endpoint's path.
	 */
	@ControllerAdvice
	public static class ActuatorEndpointLinksAdvice
			implements ResponseBodyAdvice<Object> {

		@Autowired
		private MvcEndpoints endpoints;

		@Autowired(required = false)
		private HalJsonMvcEndpoint halJsonMvcEndpoint;

		@Autowired
		private ManagementServerProperties management;

		private LinksEnhancer linksEnhancer;

		@PostConstruct
		public void init() {
			this.linksEnhancer = new LinksEnhancer(this.management.getContextPath(),
					this.endpoints);
		}

		@Override
		public boolean supports(MethodParameter returnType,
				Class<? extends HttpMessageConverter<?>> converterType) {
			returnType.increaseNestingLevel();
			Type nestedType = returnType.getNestedGenericParameterType();
			returnType.decreaseNestingLevel();
			return ResourceSupport.class.isAssignableFrom(returnType.getParameterType())
					|| TypeUtils.isAssignable(ResourceSupport.class, nestedType);
		}

		@Override
		public Object beforeBodyWrite(Object body, MethodParameter returnType,
				MediaType selectedContentType,
				Class<? extends HttpMessageConverter<?>> selectedConverterType,
				ServerHttpRequest request, ServerHttpResponse response) {
			if (request instanceof ServletServerHttpRequest) {
				beforeBodyWrite(body, (ServletServerHttpRequest) request);
			}
			return body;
		}

		private void beforeBodyWrite(Object body, ServletServerHttpRequest request) {
			Object pattern = request.getServletRequest()
					.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
			if (pattern != null && body instanceof ResourceSupport) {
				beforeBodyWrite(pattern.toString(), (ResourceSupport) body);
			}
		}

		private void beforeBodyWrite(String path, ResourceSupport body) {
			if (isActuatorEndpointPath(path)) {
				this.linksEnhancer.addEndpointLinks(body,
						this.halJsonMvcEndpoint.getPath());
			}
		}

		private boolean isActuatorEndpointPath(String path) {
			return this.halJsonMvcEndpoint != null && (this.management.getContextPath()
					+ this.halJsonMvcEndpoint.getPath()).equals(path);
		}

	}

	/**
	 * Controller advice that adds links to the existing Actuator endpoints. By default
	 * all the top-level resources are enhanced with a "self" link. Those resources that
	 * could not be enhanced (e.g. "/env/{name}") because their values are "primitive" are
	 * ignored. Those that have values of type Collection (e.g. /trace) are transformed in
	 * to maps, and the original collection value is added with a key equal to the
	 * endpoint name.
	 */
	@ControllerAdvice(assignableTypes = MvcEndpoint.class)
	public static class MvcEndpointAdvice implements ResponseBodyAdvice<Object> {

		@Autowired
		private List<RequestMappingHandlerAdapter> handlerAdapters;

		private Map<MediaType, HttpMessageConverter<?>> converterCache = new ConcurrentHashMap<MediaType, HttpMessageConverter<?>>();

		@Override
		public boolean supports(MethodParameter returnType,
				Class<? extends HttpMessageConverter<?>> converterType) {
			Class<?> controllerType = returnType.getDeclaringClass();
			return !HalJsonMvcEndpoint.class.isAssignableFrom(controllerType);
		}

		@Override
		public Object beforeBodyWrite(Object body, MethodParameter returnType,
				MediaType selectedContentType,
				Class<? extends HttpMessageConverter<?>> selectedConverterType,
				ServerHttpRequest request, ServerHttpResponse response) {
			if (request instanceof ServletServerHttpRequest) {
				return beforeBodyWrite(body, returnType, selectedContentType,
						selectedConverterType, (ServletServerHttpRequest) request,
						response);
			}
			return body;
		}

		private Object beforeBodyWrite(Object body, MethodParameter returnType,
				MediaType selectedContentType,
				Class<? extends HttpMessageConverter<?>> selectedConverterType,
				ServletServerHttpRequest request, ServerHttpResponse response) {
			if (body == null || body instanceof Resource) {
				// Assume it already was handled or it already has its links
				return body;
			}
			HttpMessageConverter<Object> converter = findConverter(selectedConverterType,
					selectedContentType);
			if (converter == null || isHypermediaDisabled(returnType)) {
				// Not a resource that can be enhanced with a link
				return body;
			}
			String path = getPath(request);
			try {
				converter.write(new EndpointResource(body, path), selectedContentType,
						response);
			}
			catch (IOException ex) {
				throw new HttpMessageNotWritableException("Cannot write response", ex);
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		private HttpMessageConverter<Object> findConverter(
				Class<? extends HttpMessageConverter<?>> selectedConverterType,
				MediaType mediaType) {
			if (this.converterCache.containsKey(mediaType)) {
				return (HttpMessageConverter<Object>) this.converterCache.get(mediaType);
			}
			for (RequestMappingHandlerAdapter handlerAdapter : this.handlerAdapters) {
				for (HttpMessageConverter<?> converter : handlerAdapter
						.getMessageConverters()) {
					if (selectedConverterType.isAssignableFrom(converter.getClass())
							&& converter.canWrite(EndpointResource.class, mediaType)) {
						this.converterCache.put(mediaType, converter);
						return (HttpMessageConverter<Object>) converter;
					}
				}
			}
			return null;
		}

		private boolean isHypermediaDisabled(MethodParameter returnType) {
			return AnnotationUtils.findAnnotation(returnType.getMethod(),
					HypermediaDisabled.class) != null
					|| AnnotationUtils.findAnnotation(
							returnType.getMethod().getDeclaringClass(),
							HypermediaDisabled.class) != null;
		}

		private String getPath(ServletServerHttpRequest request) {
			String path = (String) request.getServletRequest()
					.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
			return (path == null ? "" : path);
		}

	}

	@JsonInclude(content = Include.NON_NULL)
	@JacksonXmlRootElement(localName = "resource")
	private static class EndpointResource extends ResourceSupport {

		private Object content;

		private Map<String, Object> embedded;

		@SuppressWarnings("unchecked")
		EndpointResource(Object content, String path) {
			this.content = content instanceof Map ? null : content;
			this.embedded = (Map<String, Object>) (this.content == null ? content : null);
			add(linkTo(Object.class).slash(path).withSelfRel());
		}

		@JsonUnwrapped
		public Object getContent() {
			return this.content;
		}

		@JsonAnyGetter
		public Map<String, Object> getEmbedded() {
			return this.embedded;
		}

	}

}

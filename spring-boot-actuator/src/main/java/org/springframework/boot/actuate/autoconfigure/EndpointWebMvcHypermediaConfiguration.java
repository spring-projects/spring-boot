/*
 * Copyright 2015 the original author or authors.
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

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.mvc.ActuatorDocsEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HalBrowserEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HypermediaDisabled;
import org.springframework.boot.actuate.endpoint.mvc.LinksEnhancer;
import org.springframework.boot.actuate.endpoint.mvc.LinksMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
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
import org.springframework.util.StringUtils;
import org.springframework.util.TypeUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Autoconfiguration for hypermedia in HTTP endpoints.
 *
 * @author Dave Syer
 *
 */
@Configuration
@ConditionalOnClass(Link.class)
@ConditionalOnWebApplication
@ConditionalOnBean(HttpMessageConverters.class)
@ConditionalOnProperty(value = "endpoints.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ResourceProperties.class)
public class EndpointWebMvcHypermediaConfiguration {

	@Bean
	@ConditionalOnProperty(value = "endpoints.hal.enabled", matchIfMissing = true)
	@ConditionalOnResource(resources = "classpath:/META-INF/resources/webjars/hal-browser/b7669f1-1")
	@Conditional(MissingSpringDataRestResourceCondition.class)
	public HalBrowserEndpoint halBrowserMvcEndpoint(
			ManagementServerProperties management, ResourceProperties resources) {
		return new HalBrowserEndpoint(management,
				resources.getWelcomePage() != null ? "/hal" : "");
	}

	@Bean
	@ConditionalOnProperty(value = "endpoints.docs.enabled", matchIfMissing = true)
	@ConditionalOnResource(resources = "classpath:/META-INF/resources/spring-boot-actuator/docs/index.html")
	public ActuatorDocsEndpoint actuatorDocsEndpoint(ManagementServerProperties management) {
		return new ActuatorDocsEndpoint(management);
	}

	@Bean
	@ConditionalOnBean(ActuatorDocsEndpoint.class)
	@ConditionalOnMissingBean(CurieProvider.class)
	@ConditionalOnProperty(value = "endpoints.docs.curies.enabled", matchIfMissing = false)
	public DefaultCurieProvider curieProvider(ServerProperties server,
			ManagementServerProperties management, ActuatorDocsEndpoint endpoint) {
		String path = management.getContextPath() + endpoint.getPath()
				+ "/#spring_boot_actuator__{rel}";
		if (server.getPort() == management.getPort() && management.getPort() != null
				&& management.getPort() != 0) {
			path = server.getPath(path);
		}
		return new DefaultCurieProvider("boot", new UriTemplate(path));
	}

	@Configuration("EndpointHypermediaAutoConfiguration.MissingResourceCondition")
	@ConditionalOnResource(resources = "classpath:/META-INF/spring-data-rest/hal-browser/index.html")
	protected static class MissingSpringDataRestResourceCondition extends
	SpringBootCondition {
		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			if (context.getRegistry().containsBeanDefinition(
					"EndpointHypermediaAutoConfiguration.MissingResourceCondition")) {
				return ConditionOutcome.noMatch("Spring Data REST HAL browser found");
			}
			return ConditionOutcome.match("Spring Data REST HAL browser not found");
		}
	}

	@ConditionalOnProperty(value = "endpoints.links.enabled", matchIfMissing = true)
	public static class LinksConfiguration {

		@Bean
		public LinksMvcEndpoint linksMvcEndpoint(ResourceProperties resources) {
			return new LinksMvcEndpoint(resources.getWelcomePage() != null ? "/links"
					: "");
		}

		/**
		 * Controller advice that adds links to the home page and/or the management
		 * context path. The home page is enhanced if it is composed already of a
		 * {@link ResourceSupport} (e.g. when using Spring Data REST).
		 *
		 * @author Dave Syer
		 *
		 */
		@ControllerAdvice
		public static class HomePageLinksAdvice implements ResponseBodyAdvice<Object> {

			@Autowired
			MvcEndpoints endpoints;

			@Autowired
			LinksMvcEndpoint linksEndpoint;

			@Autowired
			ManagementServerProperties management;

			private LinksEnhancer linksEnhancer;

			@PostConstruct
			public void init() {
				this.linksEnhancer = new LinksEnhancer(this.endpoints,
						this.management.getContextPath());
			}

			@Override
			public boolean supports(MethodParameter returnType,
					Class<? extends HttpMessageConverter<?>> converterType) {
				Class<?> controllerType = returnType.getDeclaringClass();
				if (!LinksMvcEndpoint.class.isAssignableFrom(controllerType)
						&& MvcEndpoint.class.isAssignableFrom(controllerType)) {
					return false;
				}
				returnType.increaseNestingLevel();
				Type nestedType = returnType.getNestedGenericParameterType();
				returnType.decreaseNestingLevel();
				return ResourceSupport.class.isAssignableFrom(returnType
						.getParameterType())
						|| TypeUtils.isAssignable(ResourceSupport.class, nestedType);
			}

			@Override
			public Object beforeBodyWrite(Object body, MethodParameter returnType,
					MediaType selectedContentType,
					Class<? extends HttpMessageConverter<?>> selectedConverterType,
							ServerHttpRequest request, ServerHttpResponse response) {
				HttpServletRequest servletRequest = null;
				if (request instanceof ServletServerHttpRequest) {
					servletRequest = ((ServletServerHttpRequest) request)
							.getServletRequest();
					Object pattern = servletRequest
							.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
					if (pattern != null) {
						String path = pattern.toString();
						if (isHomePage(path) || isManagementPath(path)
								|| isLinksPath(path)) {
							ResourceSupport resource = (ResourceSupport) body;
							if (isHomePage(path) && hasManagementPath()) {
								String rel = this.management.getContextPath()
										.substring(1);
								resource.add(linkTo(
										EndpointWebMvcHypermediaConfiguration.class)
										.slash(this.management.getContextPath()).withRel(
												rel));
							}
							else {
								this.linksEnhancer.addEndpointLinks(resource, "");
							}
						}
					}
				}
				return body;
			}

			private boolean hasManagementPath() {
				return StringUtils.hasText(this.management.getContextPath());
			}

			private boolean isManagementPath(String path) {
				return this.management.getContextPath().equals(path);
			}

			private boolean isLinksPath(String path) {
				return (this.management.getContextPath() + this.linksEndpoint.getPath())
						.equals(path);
			}

			private boolean isHomePage(String path) {
				return "".equals(path) || "/".equals(path);
			}

		}

		/**
		 * Controller advice that adds links to the existing Actuator endpoints. By default
		 * all the top-level resources are enhanced with a "self" link. Those resources that
		 * could not be enhanced (e.g. "/env/{name}") because their values are "primitive" are
		 * ignored. Those that have values of type Collection (e.g. /trace) are transformed in
		 * to maps, and the original collection value is added with a key equal to the
		 * endpoint name.
		 *
		 * @author Dave Syer
		 *
		 */
		@ControllerAdvice(assignableTypes = MvcEndpoint.class)
		public static class MvcEndpointAdvice implements ResponseBodyAdvice<Object> {

			@Autowired
			ManagementServerProperties management;

			@Autowired
			HttpMessageConverters converters;

			private Map<MediaType, HttpMessageConverter<?>> converterCache = new ConcurrentHashMap<MediaType, HttpMessageConverter<?>>();

			@Autowired
			ObjectMapper mapper;

			@Override
			public boolean supports(MethodParameter returnType,
					Class<? extends HttpMessageConverter<?>> converterType) {
				Class<?> controllerType = returnType.getDeclaringClass();
				return !LinksMvcEndpoint.class.isAssignableFrom(controllerType)
						&& !HalBrowserEndpoint.class.isAssignableFrom(controllerType);
			}

			@Override
			public Object beforeBodyWrite(Object body, MethodParameter returnType,
					MediaType selectedContentType,
					Class<? extends HttpMessageConverter<?>> selectedConverterType,
							ServerHttpRequest request, ServerHttpResponse response) {

				if (body == null) {
					// Assume it already was handled
					return body;
				}

				if (body instanceof Resource) {
					// Assume it already has its links
					return body;
				}

				@SuppressWarnings("unchecked")
				HttpMessageConverter<Object> converter = (HttpMessageConverter<Object>) findConverter(
						selectedConverterType, selectedContentType);
				if (converter == null) {
					// Not a resource that can be enhanced with a link
					return body;
				}
				if (AnnotationUtils.findAnnotation(returnType.getMethod(),
						HypermediaDisabled.class) != null
						|| AnnotationUtils.findAnnotation(returnType.getMethod()
								.getDeclaringClass(), HypermediaDisabled.class) != null) {
					return body;
				}

				HttpServletRequest servletRequest = null;
				if (request instanceof ServletServerHttpRequest) {
					servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
					String path = (String) servletRequest
							.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
					if (path == null) {
						path = "";
					}
					try {
						converter.write(new EndpointResource(body, path),
								selectedContentType, response);
					}
					catch (IOException e) {
						throw new HttpMessageNotWritableException("Cannot write response", e);
					}
					return null;
				}
				else {
					return body;
				}

			}

			private HttpMessageConverter<?> findConverter(
					Class<? extends HttpMessageConverter<?>> selectedConverterType,
							MediaType mediaType) {
				if (this.converterCache.containsKey(mediaType)) {
					return this.converterCache.get(mediaType);
				}
				for (HttpMessageConverter<?> converter : this.converters) {
					if (selectedConverterType.isAssignableFrom(converter.getClass())
							&& converter.canWrite(EndpointResource.class, mediaType)) {
						this.converterCache.put(mediaType, converter);
						return converter;
					}
				}
				return null;
			}

		}

	}

	@JsonInclude(content = Include.NON_NULL)
	@JacksonXmlRootElement(localName = "resource")
	private static class EndpointResource extends ResourceSupport {

		private Object content;

		private Map<String, Object> embedded;

		@SuppressWarnings("unchecked")
		public EndpointResource(Object content, String path) {
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

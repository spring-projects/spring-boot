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

package org.springframework.boot.actuate.autoconfigure.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.EndpointPathProvider;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.security.ApplicationContextRequestMatcher;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

/**
 * Factory that can be used to create a {@link RequestMatcher} for actuator endpoint
 * locations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class EndpointRequest {

	private EndpointRequest() {
	}

	/**
	 * Returns a matcher that includes all {@link Endpoint actuator endpoints}. The
	 * {@link EndpointRequestMatcher#excluding(Class...) excluding} method can be used to
	 * further remove specific endpoints if required. For example: <pre class="code">
	 * EndpointRequestMatcher.toAnyEndpoint().excluding(ShutdownEndpoint.class)
	 * </pre>
	 * @return the configured {@link RequestMatcher}
	 */
	public static EndpointRequestMatcher toAnyEndpoint() {
		return new EndpointRequestMatcher();
	}

	/**
	 * Returns a matcher that includes the specified {@link Endpoint actuator endpoints}.
	 * For example: <pre class="code">
	 * EndpointRequestMatcher.to(ShutdownEndpoint.class, HealthEndpoint.class)
	 * </pre>
	 * @param endpoints the endpoints to include
	 * @return the configured {@link RequestMatcher}
	 */
	public static EndpointRequestMatcher to(Class<?>... endpoints) {
		return new EndpointRequestMatcher(endpoints);
	}

	/**
	 * Returns a matcher that includes the specified {@link Endpoint actuator endpoints}.
	 * For example: <pre class="code">
	 * EndpointRequestMatcher.to("shutdown", "health")
	 * </pre>
	 * @param endpoints the endpoints to include
	 * @return the configured {@link RequestMatcher}
	 */
	public static EndpointRequestMatcher to(String... endpoints) {
		return new EndpointRequestMatcher(endpoints);
	}

	/**
	 * The request matcher used to match against {@link Endpoint actuator endpoints}.
	 */
	public final static class EndpointRequestMatcher
			extends ApplicationContextRequestMatcher<EndpointPathProvider> {

		private final List<Object> includes;

		private final List<Object> excludes;

		private RequestMatcher delegate;

		private EndpointRequestMatcher() {
			super(EndpointPathProvider.class);
			this.includes = Collections.emptyList();
			this.excludes = Collections.emptyList();
		}

		private EndpointRequestMatcher(Class<?>[] endpoints) {
			super(EndpointPathProvider.class);
			this.includes = Arrays.asList((Object[]) endpoints);
			this.excludes = Collections.emptyList();
		}

		private EndpointRequestMatcher(String[] endpoints) {
			super(EndpointPathProvider.class);
			this.includes = Arrays.asList((Object[]) endpoints);
			this.excludes = Collections.emptyList();
		}

		private EndpointRequestMatcher(List<Object> includes, List<Object> excludes) {
			super(EndpointPathProvider.class);
			this.includes = includes;
			this.excludes = excludes;
		}

		EndpointRequestMatcher excluding(Class<?>... endpoints) {
			List<Object> excludes = new ArrayList<>(this.excludes);
			excludes.addAll(Arrays.asList((Object[]) endpoints));
			return new EndpointRequestMatcher(this.includes, excludes);
		}

		EndpointRequestMatcher excluding(String... endpoints) {
			List<Object> excludes = new ArrayList<>(this.excludes);
			excludes.addAll(Arrays.asList((Object[]) endpoints));
			return new EndpointRequestMatcher(this.includes, excludes);
		}

		@Override
		protected void initialized(EndpointPathProvider endpointPathProvider) {
			Set<String> paths = new LinkedHashSet<>(this.includes.isEmpty()
					? endpointPathProvider.getPaths() : Collections.emptyList());
			streamPaths(this.includes, endpointPathProvider).forEach(paths::add);
			streamPaths(this.excludes, endpointPathProvider).forEach(paths::remove);
			this.delegate = new OrRequestMatcher(getDelegateMatchers(paths));
		}

		private Stream<String> streamPaths(List<Object> source,
				EndpointPathProvider endpointPathProvider) {
			return source.stream().filter(Objects::nonNull).map(this::getPathId)
					.map(endpointPathProvider::getPath);
		}

		private String getPathId(Object source) {
			if (source instanceof String) {
				return (String) source;
			}
			if (source instanceof Class) {
				return getPathId((Class<?>) source);
			}
			throw new IllegalStateException("Unsupported source " + source);
		}

		private String getPathId(Class<?> source) {
			Endpoint annotation = AnnotationUtils.findAnnotation(source, Endpoint.class);
			Assert.state(annotation != null,
					() -> "Class " + source + " is not annotated with @Endpoint");
			return annotation.id();
		}

		private List<RequestMatcher> getDelegateMatchers(Set<String> paths) {
			return paths.stream().map((path) -> new AntPathRequestMatcher(path + "/**"))
					.collect(Collectors.toList());
		}

		@Override
		public boolean matches(HttpServletRequest request) {
			try {
				return super.matches(request);
			}
			catch (BeanCreationException ex) {
				return false;
			}
		}

		@Override
		protected boolean matches(HttpServletRequest request,
				EndpointPathProvider context) {
			return this.delegate.matches(request);
		}

	}

}

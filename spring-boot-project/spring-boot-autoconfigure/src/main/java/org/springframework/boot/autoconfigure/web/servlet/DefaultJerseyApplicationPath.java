/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.autoconfigure.web.servlet;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.boot.autoconfigure.jersey.JerseyProperties;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link JerseyApplicationPath} that derives the path from
 * {@link JerseyProperties} or the {@code @ApplicationPath} annotation.
 *
 * @author Madhura Bhave
 */
public class DefaultJerseyApplicationPath implements JerseyApplicationPath {

	private final String applicationPath;

	private final ResourceConfig config;

	public DefaultJerseyApplicationPath(String applicationPath, ResourceConfig config) {
		this.applicationPath = applicationPath;
		this.config = config;
	}

	@Override
	public String getPath() {
		return resolveApplicationPath();
	}

	private String resolveApplicationPath() {
		if (StringUtils.hasLength(this.applicationPath)) {
			return this.applicationPath;
		}
		return findApplicationPath(AnnotationUtils.findAnnotation(
				this.config.getApplication().getClass(), ApplicationPath.class));
	}

	private static String findApplicationPath(ApplicationPath annotation) {
		// Jersey doesn't like to be the default servlet, so map to /* as a fallback
		if (annotation == null) {
			return "/*";
		}
		return annotation.value();
	}

}

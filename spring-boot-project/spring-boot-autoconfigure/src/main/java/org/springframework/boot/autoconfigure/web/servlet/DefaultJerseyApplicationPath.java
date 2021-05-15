/*
 * Copyright 2012-2020 the original author or authors.
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
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link JerseyApplicationPath} that derives the path from
 * {@link JerseyProperties} or the {@code @ApplicationPath} annotation.
 *
 * @author Madhura Bhave
 * @since 2.1.0
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
		// Jersey doesn't like to be the default servlet, so map to /* as a fallback
		return MergedAnnotations.from(this.config.getApplication().getClass(), SearchStrategy.TYPE_HIERARCHY)
				.get(ApplicationPath.class).getValue(MergedAnnotation.VALUE, String.class).orElse("/*");
	}

}

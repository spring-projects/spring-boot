/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.web.mappings.servlet;

import org.springframework.web.servlet.DispatcherServlet;

/**
 * A description of a mapping known to a {@link DispatcherServlet}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class DispatcherServletMappingDescription {

	private final String handler;

	private final String predicate;

	private final DispatcherServletMappingDetails details;

	DispatcherServletMappingDescription(String predicate, String handler,
			DispatcherServletMappingDetails details) {
		this.handler = handler;
		this.predicate = predicate;
		this.details = details;
	}

	public String getHandler() {
		return this.handler;
	}

	public String getPredicate() {
		return this.predicate;
	}

	public DispatcherServletMappingDetails getDetails() {
		return this.details;
	}

}

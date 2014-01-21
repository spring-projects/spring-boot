/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.web;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.RequestAttributes;

/**
 * Marker interface used to indicate that a {@link Controller @Controller} is used to
 * render errors.
 * 
 * @author Phillip Webb
 */
public interface ErrorController {

	/**
	 * Returns the path of the error page.
	 */
	public String getErrorPath();

	/**
	 * Extract a useful model of the error from the request attributes.
	 * @param attributes the request attributes
	 * @param trace flag to indicate that stack trace information should be included
	 * @return a model containing error messages and codes etc.
	 */
	public Map<String, Object> extract(RequestAttributes attributes, boolean trace);

}

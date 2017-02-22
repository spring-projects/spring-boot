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

package org.springframework.boot.context.embedded;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Configuration for Servlet containers.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class Servlet {

	/**
	 * ServletContext parameters.
	 */
	private final Map<String, String> contextParameters = new HashMap<String, String>();

	/**
	 * Context path of the application.
	 */
	private String contextPath;

	/**
	 * Path of the main dispatcher servlet.
	 */
	private String path = "/";

	@NestedConfigurationProperty
	private Jsp jsp = new Jsp();

	public String getContextPath() {
		return this.contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = cleanContextPath(contextPath);
	}

	private String cleanContextPath(String contextPath) {
		if (StringUtils.hasText(contextPath) && contextPath.endsWith("/")) {
			return contextPath.substring(0, contextPath.length() - 1);
		}
		return contextPath;
	}

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		Assert.notNull(path, "Path must not be null");
		this.path = path;
	}

	public Map<String, String> getContextParameters() {
		return this.contextParameters;
	}

	public Jsp getJsp() {
		return this.jsp;
	}

	public void setJsp(Jsp jsp) {
		this.jsp = jsp;
	}

	public String getServletMapping() {
		if (this.path.equals("") || this.path.equals("/")) {
			return "/";
		}
		if (this.path.contains("*")) {
			return this.path;
		}
		if (this.path.endsWith("/")) {
			return this.path + "*";
		}
		return this.path + "/*";
	}

	public String getPath(String path) {
		String prefix = getServletPrefix();
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return prefix + path;
	}

	public String getServletPrefix() {
		String result = this.path;
		if (result.contains("*")) {
			result = result.substring(0, result.indexOf("*"));
		}
		if (result.endsWith("/")) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	public String[] getPathsArray(Collection<String> paths) {
		String[] result = new String[paths.size()];
		int i = 0;
		for (String path : paths) {
			result[i++] = getPath(path);
		}
		return result;
	}

	public String[] getPathsArray(String[] paths) {
		String[] result = new String[paths.length];
		int i = 0;
		for (String path : paths) {
			result[i++] = getPath(path);
		}
		return result;
	}


}

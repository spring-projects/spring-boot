/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.web.servlet.server;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the server's JSP servlet.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class Jsp {

	/**
	 * Class name of the servlet to use for JSPs. If registered is true and this class is
	 * on the classpath then it will be registered.
	 */
	private String className = "org.apache.jasper.servlet.JspServlet";

	private Map<String, String> initParameters = new HashMap<>();

	/**
	 * Whether the JSP servlet is registered.
	 */
	private boolean registered = true;

	public Jsp() {
		this.initParameters.put("development", "false");
	}

	/**
	 * Return the class name of the servlet to use for JSPs. If {@link #getRegistered()
	 * registered} is {@code true} and this class is on the classpath then it will be
	 * registered.
	 * @return the class name of the servlet to use for JSPs
	 */
	public String getClassName() {
		return this.className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	/**
	 * Return the init parameters used to configure the JSP servlet.
	 * @return the init parameters
	 */
	public Map<String, String> getInitParameters() {
		return this.initParameters;
	}

	public void setInitParameters(Map<String, String> initParameters) {
		this.initParameters = initParameters;
	}

	/**
	 * Return whether the JSP servlet is registered.
	 * @return {@code true} to register the JSP servlet
	 */
	public boolean getRegistered() {
		return this.registered;
	}

	public void setRegistered(boolean registered) {
		this.registered = registered;
	}

}

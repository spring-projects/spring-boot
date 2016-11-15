/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the container's JSP servlet.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
public class JspServlet {

	/**
	 * The class name of the servlet to use for JSPs. If registered is true and this class
	 * is on the classpath then it will be registered. Since both Tomcat and Jetty use
	 * Jasper for their JSP implementation the default is
	 * org.apache.jasper.servlet.JspServlet.
	 */
	private String className = "org.apache.jasper.servlet.JspServlet";

	/**
	 * Init parameters used to configure the JSP servlet.
	 */
	private Map<String, String> initParameters = new HashMap<String, String>();

	/**
	 * Whether or not the JSP servlet should be registered with the embedded servlet
	 * container.
	 */
	private boolean registered = true;

	public JspServlet() {
		this.initParameters.put("development", "false");
	}

	public String getClassName() {
		return this.className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public Map<String, String> getInitParameters() {
		return this.initParameters;
	}

	public void setInitParameters(Map<String, String> initParameters) {
		this.initParameters = initParameters;
	}

	public boolean getRegistered() {
		return this.registered;
	}

	public void setRegistered(boolean registered) {
		this.registered = registered;
	}

}

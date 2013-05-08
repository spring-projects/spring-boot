/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.bootstrap.context.embedded;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.Registration;

import org.springframework.core.Conventions;
import org.springframework.util.Assert;

/**
 * Base class for {@link ServletRegistrationBean servlet} and
 * {@link FilterRegistrationBean filter} registration beans.
 * 
 * @author Phillip Webb
 * @since 4.0
 * @see ServletRegistrationBean
 * @see FilterRegistrationBean
 */
public abstract class RegistrationBean implements ServletContextInitializer {

	private String name;

	private boolean asyncSupported = true;

	private Map<String, String> initParameters = new LinkedHashMap<String, String>();

	/**
	 * Set the name of this registration. If not specified the bean name will be used.
	 */
	public void setName(String name) {
		Assert.hasLength(name, "Name must not be empty");
		this.name = name;
	}

	/**
	 * Sets if asynchronous operations are support for this registration. If not specified
	 * defaults to {@code true}.
	 */
	public void setAsyncSupported(boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	/**
	 * Returns if asynchronous operations are support for this registration.
	 */
	public boolean isAsyncSupported() {
		return asyncSupported;
	}

	/**
	 * Set init-parameters for this registration. Calling this method will replace any
	 * existing init-parameters.
	 * @see #getInitParameters
	 * @see #addInitParameter
	 */
	public void setInitParameters(Map<String, String> initParameters) {
		Assert.notNull(initParameters, "InitParameters must not be null");
		this.initParameters = new LinkedHashMap<String, String>(initParameters);
	}

	/**
	 * Returns a mutable Map of the registration init-parameters.
	 */
	public Map<String, String> getInitParameters() {
		return this.initParameters;
	}

	/**
	 * Add a single init-parameter, replacing any existing parameter with the same name.
	 * @param name the init-parameter name
	 * @param value the init-parameter value
	 */
	public void addInitParameter(String name, String value) {
		Assert.notNull(name, "Name must not be null");
		this.initParameters.put(name, value);
	}

	/**
	 * Deduces the name for this registration. Will return user specified name or fallback
	 * to convention based naming.
	 * @param value the object used for convention based names
	 */
	protected final String getOrDeduceName(Object value) {
		return (this.name != null ? this.name : Conventions.getVariableName(value));
	}

	/**
	 * Configure registration base settings.
	 */
	protected void configure(Registration.Dynamic registration) {
		registration.setAsyncSupported(this.asyncSupported);
		if (this.initParameters.size() > 0) {
			registration.setInitParameters(this.initParameters);
		}
	}

}

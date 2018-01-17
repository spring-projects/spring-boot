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

package org.springframework.boot.web.servlet;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.Registration;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Conventions;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for Servlet 3.0+ {@link javax.servlet.Registration.Dynamic dynamic} based
 * registration beans.
 *
 * @param <D> The dynamic registration result
 * @author Phillip Webb
 * @since 2.0.0
 */
public abstract class DynamicRegistrationBean<D extends Registration.Dynamic>
		extends RegistrationBean {

	private static final Log logger = LogFactory.getLog(RegistrationBean.class);

	private String name;

	private boolean asyncSupported = true;

	private Map<String, String> initParameters = new LinkedHashMap<>();

	/**
	 * Set the name of this registration. If not specified the bean name will be used.
	 * @param name the name of the registration
	 */
	public void setName(String name) {
		Assert.hasLength(name, "Name must not be empty");
		this.name = name;
	}

	/**
	 * Sets if asynchronous operations are supported for this registration. If not
	 * specified defaults to {@code true}.
	 * @param asyncSupported if async is supported
	 */
	public void setAsyncSupported(boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	/**
	 * Returns if asynchronous operations are supported for this registration.
	 * @return if async is supported
	 */
	public boolean isAsyncSupported() {
		return this.asyncSupported;
	}

	/**
	 * Set init-parameters for this registration. Calling this method will replace any
	 * existing init-parameters.
	 * @param initParameters the init parameters
	 * @see #getInitParameters
	 * @see #addInitParameter
	 */
	public void setInitParameters(Map<String, String> initParameters) {
		Assert.notNull(initParameters, "InitParameters must not be null");
		this.initParameters = new LinkedHashMap<>(initParameters);
	}

	/**
	 * Returns a mutable Map of the registration init-parameters.
	 * @return the init parameters
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

	@Override
	protected final void register(String description, ServletContext servletContext) {
		D registration = addRegistration(description, servletContext);
		if (registration == null) {
			logger.info(StringUtils.capitalize(description) + " was not registered "
					+ "(possibly already registered?)");
			return;
		}
		configure(registration);
	}

	protected abstract D addRegistration(String description,
			ServletContext servletContext);

	protected void configure(D registration) {
		registration.setAsyncSupported(this.asyncSupported);
		if (!this.initParameters.isEmpty()) {
			registration.setInitParameters(this.initParameters);
		}
	}

	/**
	 * Deduces the name for this registration. Will return user specified name or fallback
	 * to convention based naming.
	 * @param value the object used for convention based names
	 * @return the deduced name
	 */
	protected final String getOrDeduceName(Object value) {
		return (this.name != null ? this.name : Conventions.getVariableName(value));
	}

}

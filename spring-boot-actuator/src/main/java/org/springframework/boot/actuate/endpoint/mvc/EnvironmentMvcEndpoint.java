/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Dave Syer
 */
public class EnvironmentMvcEndpoint extends GenericMvcEndpoint implements
		EnvironmentAware {

	private Environment environment;

	public EnvironmentMvcEndpoint(EnvironmentEndpoint delegate) {
		super(delegate);
	}

	@RequestMapping(value = "/{name:.*}", method = RequestMethod.GET)
	@ResponseBody
	public Object value(@PathVariable String name) {
		String result = this.environment.getProperty(name);
		if (result == null) {
			throw new NoSuchPropertyException("No such property: " + name);
		}
		return EnvironmentEndpoint.sanitize(name, result);
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such property")
	public static class NoSuchPropertyException extends RuntimeException {

		public NoSuchPropertyException(String string) {
			super(string);
		}

	}
}

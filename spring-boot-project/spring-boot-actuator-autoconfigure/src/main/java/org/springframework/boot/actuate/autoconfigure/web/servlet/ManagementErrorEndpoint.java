/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.servlet;

import java.util.Map;

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * {@link Controller @Controller} for handling "/error" path when the management servlet
 * is in a child context. The regular {@link ErrorController} should be available there
 * but because of the way the handler mappings are set up it will not be detected.
 *
 * @author Dave Syer
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @since 2.0.0
 */
@Controller
public class ManagementErrorEndpoint {

	private final ErrorAttributes errorAttributes;

	private final ErrorProperties errorProperties;

	public ManagementErrorEndpoint(ErrorAttributes errorAttributes, ErrorProperties errorProperties) {
		Assert.notNull(errorAttributes, "'errorAttributes' must not be null");
		Assert.notNull(errorProperties, "'errorProperties' must not be null");
		this.errorAttributes = errorAttributes;
		this.errorProperties = errorProperties;
	}

	@RequestMapping("${server.error.path:${error.path:/error}}")
	@ResponseBody
	public Map<String, Object> invoke(ServletWebRequest request) {
		return this.errorAttributes.getErrorAttributes(request, getErrorAttributeOptions(request));
	}

	private ErrorAttributeOptions getErrorAttributeOptions(ServletWebRequest request) {
		ErrorAttributeOptions options = ErrorAttributeOptions.defaults();
		if (this.errorProperties.isIncludeException()) {
			options = options.including(Include.EXCEPTION);
		}
		if (includeStackTrace(request)) {
			options = options.including(Include.STACK_TRACE);
		}
		if (includeMessage(request)) {
			options = options.including(Include.MESSAGE);
		}
		if (includeBindingErrors(request)) {
			options = options.including(Include.BINDING_ERRORS);
		}
		options = includePath(request) ? options.including(Include.PATH) : options.excluding(Include.PATH);
		return options;
	}

	private boolean includeStackTrace(ServletWebRequest request) {
		return switch (this.errorProperties.getIncludeStacktrace()) {
			case ALWAYS -> true;
			case ON_PARAM -> getBooleanParameter(request, "trace");
			case NEVER -> false;
		};
	}

	private boolean includeMessage(ServletWebRequest request) {
		return switch (this.errorProperties.getIncludeMessage()) {
			case ALWAYS -> true;
			case ON_PARAM -> getBooleanParameter(request, "message");
			case NEVER -> false;
		};
	}

	private boolean includeBindingErrors(ServletWebRequest request) {
		return switch (this.errorProperties.getIncludeBindingErrors()) {
			case ALWAYS -> true;
			case ON_PARAM -> getBooleanParameter(request, "errors");
			case NEVER -> false;
		};
	}

	private boolean includePath(ServletWebRequest request) {
		return switch (this.errorProperties.getIncludePath()) {
			case ALWAYS -> true;
			case ON_PARAM -> getBooleanParameter(request, "path");
			case NEVER -> false;
		};
	}

	protected boolean getBooleanParameter(ServletWebRequest request, String parameterName) {
		String parameter = request.getParameter(parameterName);
		if (parameter == null) {
			return false;
		}
		return !"false".equalsIgnoreCase(parameter);
	}

}

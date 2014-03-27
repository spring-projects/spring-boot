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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

/**
 * Basic global error {@link Controller}, rendering servlet container error codes and
 * messages where available. More specific errors can be handled either using Spring MVC
 * abstractions (e.g. {@code @ExceptionHandler}) or by adding servlet
 * {@link AbstractEmbeddedServletContainerFactory#setErrorPages(java.util.Set) container
 * error pages}.
 * 
 * @author Dave Syer
 */
@Controller
public class BasicErrorController implements ErrorController {

	private static final String ERROR_KEY = "error";

	private final Log logger = LogFactory.getLog(BasicErrorController.class);

	@Value("${error.path:/error}")
	private String errorPath;

	@Override
	public String getErrorPath() {
		return this.errorPath;
	}

	@RequestMapping(value = "${error.path:/error}", produces = "text/html")
	public ModelAndView errorHtml(HttpServletRequest request) {
		Map<String, Object> map = extract(new ServletRequestAttributes(request), false,
				false);
		return new ModelAndView(ERROR_KEY, map);
	}

	@RequestMapping(value = "${error.path:/error}")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
		ServletRequestAttributes attributes = new ServletRequestAttributes(request);
		String trace = request.getParameter("trace");
		Map<String, Object> extracted = extract(attributes,
				trace != null && !"false".equals(trace.toLowerCase()), true);
		HttpStatus statusCode = getStatus((Integer) extracted.get("status"));
		return new ResponseEntity<Map<String, Object>>(extracted, statusCode);
	}

	private HttpStatus getStatus(Integer value) {
		try {
			return HttpStatus.valueOf(value);
		}
		catch (Exception ex) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
	}

	@Override
	public Map<String, Object> extract(RequestAttributes attributes, boolean trace,
			boolean log) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("timestamp", new Date());
		try {
			Throwable error = (Throwable) attributes.getAttribute(
					"javax.servlet.error.exception", RequestAttributes.SCOPE_REQUEST);
			Object obj = attributes.getAttribute("javax.servlet.error.status_code",
					RequestAttributes.SCOPE_REQUEST);
			int status = 999;
			if (obj != null) {
				status = (Integer) obj;
				map.put(ERROR_KEY, HttpStatus.valueOf(status).getReasonPhrase());
			}
			else {
				map.put(ERROR_KEY, "None");
			}
			map.put("status", status);
			if (error != null) {
				while (error instanceof ServletException && error.getCause() != null) {
					error = ((ServletException) error).getCause();
				}
				map.put("exception", error.getClass().getName());
				map.put("message", error.getMessage());
				if (trace) {
					StringWriter stackTrace = new StringWriter();
					error.printStackTrace(new PrintWriter(stackTrace));
					stackTrace.flush();
					map.put("trace", stackTrace.toString());
				}
				if (log) {
					this.logger.error(error);
				}
			}
			else {
				Object message = attributes.getAttribute("javax.servlet.error.message",
						RequestAttributes.SCOPE_REQUEST);
				map.put("message", message == null ? "No message available" : message);
			}
			return map;
		}
		catch (Exception ex) {
			map.put(ERROR_KEY, ex.getClass().getName());
			map.put("message", ex.getMessage());
			if (log) {
				this.logger.error(ex);
			}
			return map;
		}
	}

}

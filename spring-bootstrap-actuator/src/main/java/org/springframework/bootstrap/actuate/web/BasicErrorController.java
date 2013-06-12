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

package org.springframework.bootstrap.actuate.web;

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
import org.springframework.bootstrap.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
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

	private Log logger = LogFactory.getLog(BasicErrorController.class);

	@Value("${error.path:/error}")
	private String errorPath;

	@Override
	public String getErrorPath() {
		return this.errorPath;
	}

	@RequestMapping(value = "${error.path:/error}", produces = "text/html")
	public ModelAndView errorHtml(HttpServletRequest request) {
		Map<String, Object> map = error(request);
		return new ModelAndView("error", map);
	}

	@RequestMapping(value = "${error.path:/error}")
	@ResponseBody
	public Map<String, Object> error(HttpServletRequest request) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("timestamp", new Date());
		try {
			Throwable error = (Throwable) request
					.getAttribute("javax.servlet.error.exception");
			Object obj = request.getAttribute("javax.servlet.error.status_code");
			int status = 999;
			if (obj != null) {
				status = (Integer) obj;
				map.put("error", HttpStatus.valueOf(status).getReasonPhrase());
			} else {
				map.put("error", "None");
			}
			map.put("status", status);
			if (error != null) {
				while (error instanceof ServletException) {
					error = ((ServletException) error).getCause();
				}
				map.put("exception", error.getClass().getName());
				map.put("message", error.getMessage());
				String trace = request.getParameter("trace");
				if (trace != null && !"false".equals(trace.toLowerCase())) {
					StringWriter stackTrace = new StringWriter();
					error.printStackTrace(new PrintWriter(stackTrace));
					stackTrace.flush();
					map.put("trace", stackTrace.toString());
				}
				this.logger.error(error);
			} else {
				Object message = request.getAttribute("javax.servlet.error.message");
				map.put("message", message == null ? "No message available" : message);
			}
			return map;
		} catch (Exception e) {
			map.put("error", e.getClass().getName());
			map.put("message", e.getMessage());
			this.logger.error(e);
			return map;
		}
	}

}

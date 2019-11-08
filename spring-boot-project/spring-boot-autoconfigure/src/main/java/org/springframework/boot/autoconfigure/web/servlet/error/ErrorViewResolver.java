/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet.error;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interface that can be implemented by beans that resolve error views.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
@FunctionalInterface
public interface ErrorViewResolver {

	/**
	 * Resolve an error view for the specified details.
	 * @param request the source request
	 * @param status the http status of the error
	 * @param model the suggested model to be used with the view
	 * @return a resolved {@link ModelAndView} or {@code null}
	 */
	ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model);

}

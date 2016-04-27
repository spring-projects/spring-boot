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

package org.springframework.boot.context.web;

import javax.servlet.Filter;

import org.springframework.boot.web.servlet.ErrorPageRegistrar;
import org.springframework.boot.web.servlet.ErrorPageRegistry;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * A Servlet {@link Filter} that provides an {@link ErrorPageRegistry} for non-embedded
 * applications (i.e. deployed WAR files). It registers error pages and handles
 * application errors by filtering requests and forwarding to the error pages instead of
 * letting the container handle them. Error pages are a feature of the servlet spec but
 * there is no Java API for registering them in the spec. This filter works around that by
 * accepting error page registrations from Spring Boot's {@link ErrorPageRegistrar} (any
 * beans of that type in the context will be applied to this container).
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.4.0
 * @deprecated as of 1.4 in favor of org.springframework.boot.web.support.ErrorPageFilter
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Deprecated
public class ErrorPageFilter extends org.springframework.boot.web.support.ErrorPageFilter
		implements NonEmbeddedServletContainerFactory {

}

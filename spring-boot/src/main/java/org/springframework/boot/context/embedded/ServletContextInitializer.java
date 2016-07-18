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

import javax.servlet.ServletContext;

import org.springframework.web.SpringServletContainerInitializer;
import org.springframework.web.WebApplicationInitializer;

/**
 * Interface used to configure a Servlet 3.0+ {@link ServletContext context}
 * programmatically. Unlike {@link WebApplicationInitializer}, classes that implement this
 * interface (and do not implement {@link WebApplicationInitializer}) will <b>not</b> be
 * detected by {@link SpringServletContainerInitializer} and hence will not be
 * automatically bootstrapped by the Servlet container.
 * <p>
 * This interface is primarily designed to allow {@link ServletContextInitializer}s to be
 * managed by Spring and not the Servlet container.
 * <p>
 * For configuration examples see {@link WebApplicationInitializer}.
 *
 * @author Phillip Webb
 * @see WebApplicationInitializer
 * @deprecated as of 1.4 in favor of
 * org.springframework.boot.web.servlet.ServletContextInitializer
 */
@Deprecated
public interface ServletContextInitializer
		extends org.springframework.boot.web.servlet.ServletContextInitializer {

}

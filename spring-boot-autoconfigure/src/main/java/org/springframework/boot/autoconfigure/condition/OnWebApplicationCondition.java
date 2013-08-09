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

package org.springframework.boot.autoconfigure.condition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * {@link Condition} that checks for a the presence or absence of
 * {@link WebApplicationContext}.
 * 
 * @author Dave Syer
 * @see ConditionalOnWebApplication
 * @see ConditionalOnNotWebApplication
 */
class OnWebApplicationCondition implements Condition {

	private static final String WEB_CONTEXT_CLASS = "org.springframework.web.context.support.GenericWebApplicationContext";

	private static Log logger = LogFactory.getLog(OnWebApplicationCondition.class);

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		boolean webContextRequired = metadata
				.isAnnotated(ConditionalOnWebApplication.class.getName());
		boolean webApplication = isWebApplication(context, metadata);
		return (webContextRequired ? webApplication : !webApplication);
	}

	private boolean isWebApplication(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		String checking = ConditionLogUtils.getPrefix(logger, metadata);

		if (!ClassUtils.isPresent(WEB_CONTEXT_CLASS, context.getClassLoader())) {
			if (logger.isDebugEnabled()) {
				logger.debug(checking + "web application classes not found");
			}
			return false;
		}

		if (context.getBeanFactory() != null) {
			String[] scopes = context.getBeanFactory().getRegisteredScopeNames();
			if (ObjectUtils.containsElement(scopes, "session")) {
				if (logger.isDebugEnabled()) {
					logger.debug(checking + "found web application scope");
				}
				return true;
			}
		}

		if (context.getEnvironment() instanceof StandardServletEnvironment) {
			if (logger.isDebugEnabled()) {
				logger.debug(checking + "found web application environment");
			}
			return true;
		}

		if (logger.isDebugEnabled()) {
			logger.debug(checking + "is not a web application");
		}
		return false;
	}
}

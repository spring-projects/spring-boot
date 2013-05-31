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

package org.springframework.bootstrap.actuate.autoconfigure;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.bootstrap.context.annotation.ConditionLogUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;

/**
 * A condition that can determine if the bean it applies to is in the management context
 * (the application context with the management endpoints).
 * 
 * @author Dave Syer
 * 
 * @see ConditionalOnManagementContext
 */
public class OnManagementContextCondition implements Condition {

	private static Log logger = LogFactory.getLog(OnManagementContextCondition.class);

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

		String checking = ConditionLogUtils.getPrefix(logger, metadata);

		Environment environment = context.getEnvironment();
		int serverPort = environment.getProperty("server.port", Integer.class, 8080);
		int managementPort = environment.getProperty("management.port", Integer.class,
				serverPort);

		// If there is no management context, the decision is easy (match=false)
		boolean managementEnabled = managementPort > 0;

		// The management context is the same as the parent context
		boolean managementContextInParent = serverPort == managementPort;

		// The current context is a child context with a management server
		boolean managementChildContext = context.getBeanFactory().getBeanNamesForType(
				ManagementServerConfiguration.class).length > 0;

		// The management auto configuration either hasn't been added yet or has been
		// added to the context and it is enabled
		boolean containsManagementBeans = !context.getBeanFactory().containsSingleton(
				ManagementAutoConfiguration.MEMO_BEAN_NAME)
				|| (Boolean) context.getBeanFactory().getSingleton(
						ManagementAutoConfiguration.MEMO_BEAN_NAME);

		boolean result = managementEnabled
				&& ((managementContextInParent && containsManagementBeans) || managementChildContext);

		if (logger.isDebugEnabled()) {
			if (!managementEnabled) {
				logger.debug(checking + "Management context is disabled");
			} else {
				logger.debug(checking + "Management context is in parent: "
						+ managementContextInParent + " (management.port="
						+ managementPort + ", server.port=" + serverPort + ")");
				logger.debug(checking + "In management child context: "
						+ managementChildContext);
				logger.debug(checking + "In management parent context: "
						+ containsManagementBeans);
				logger.debug(checking + "Finished matching and result is matches="
						+ result);
			}
		}

		if (!result && metadata instanceof AnnotationMetadata) {
			Collection<String> beanClasses = getManagementContextClasses(context
					.getBeanFactory());
			beanClasses.add(((AnnotationMetadata) metadata).getClassName());
		}
		return result;

	}

	private Collection<String> getManagementContextClasses(
			ConfigurableListableBeanFactory beanFactory) {
		String name = OnManagementContextCondition.class.getName();
		if (!beanFactory.containsSingleton(name)) {
			beanFactory.registerSingleton(name, new HashSet<String>());
		}
		@SuppressWarnings("unchecked")
		Collection<String> result = (Collection<String>) beanFactory.getSingleton(name);
		return result;
	}

}

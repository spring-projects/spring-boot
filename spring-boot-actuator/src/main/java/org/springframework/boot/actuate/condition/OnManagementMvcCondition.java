/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.condition;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link Condition} that checks whether or not the management MVC endpoints are in the
 * main context.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
class OnManagementMvcCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		RelaxedPropertyResolver management = new RelaxedPropertyResolver(
				context.getEnvironment(), "management.");
		RelaxedPropertyResolver server = new RelaxedPropertyResolver(
				context.getEnvironment(), "server.");
		Integer managementPort = management.getProperty("port", Integer.class);
		if (managementPort == null) {
			ManagementServerProperties managementServerProperties = getBeanCarefully(
					context, ManagementServerProperties.class);
			if (managementServerProperties != null) {
				managementPort = managementServerProperties.getPort();
			}
		}
		if (managementPort != null && managementPort < 0) {
			return new ConditionOutcome(false, "The mangagement port is disabled");
		}
		if (!(context.getResourceLoader() instanceof WebApplicationContext)) {
			// Current context is not a webapp
			return new ConditionOutcome(false, "The context is not a webapp");
		}
		Integer serverPort = server.getProperty("port", Integer.class);
		if (serverPort == null) {
			ServerProperties serverProperties = getBeanCarefully(context,
					ServerProperties.class);
			if (serverProperties != null) {
				serverPort = serverProperties.getPort();
			}
		}
		if ((managementPort == null)
				|| (serverPort == null && managementPort.equals(8080))
				|| (managementPort != 0 && managementPort.equals(serverPort))) {
			return new ConditionOutcome(true,
					"The main context is the management context");
		}
		return new ConditionOutcome(false,
				"The main context is not the management context");
	}

	private <T> T getBeanCarefully(ConditionContext context, Class<T> type) {
		String[] names = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
				context.getBeanFactory(), type, false, false);
		if (names.length == 1) {
			BeanDefinition original = findBeanDefinition(context.getBeanFactory(), names[0]);
			if (original instanceof RootBeanDefinition) {
				DefaultListableBeanFactory temp = new DefaultListableBeanFactory();
				temp.setParentBeanFactory(context.getBeanFactory());
				temp.registerBeanDefinition("bean",
						((RootBeanDefinition) original).cloneBeanDefinition());
				return temp.getBean(type);
			}
			return BeanFactoryUtils.beanOfType(context.getBeanFactory(), type, false,
					false);
		}
		;
		return null;
	}

	private BeanDefinition findBeanDefinition(ConfigurableListableBeanFactory beanFactory, String name) {
		BeanDefinition original = null;
		while (beanFactory!=null && original==null){
			if (beanFactory.containsLocalBean(name)) {
				original = beanFactory.getBeanDefinition(name);
			} else {
				BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
				if (parentBeanFactory instanceof ConfigurableListableBeanFactory) {
					beanFactory = (ConfigurableListableBeanFactory) parentBeanFactory;
				} else {
					beanFactory = null;
				}
			}
		}
		return original;
	}

}

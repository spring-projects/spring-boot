/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.web.servlet;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.annotation.WebFilter;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.util.StringUtils;

/**
 * Handler for {@link WebFilter}-annotated classes.
 *
 * @author Andy Wilkinson
 */
class WebFilterHandler extends ServletComponentHandler {

	WebFilterHandler() {
		super(WebFilter.class);
	}

	@Override
	public void doHandle(Map<String, Object> attributes,
			ScannedGenericBeanDefinition beanDefinition,
			BeanDefinitionRegistry registry) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition(FilterRegistrationBean.class);
		builder.addPropertyValue("asyncSupported", attributes.get("asyncSupported"));
		builder.addPropertyValue("dispatcherTypes", extractDispatcherTypes(attributes));
		builder.addPropertyValue("filter", beanDefinition);
		builder.addPropertyValue("initParameters", extractInitParameters(attributes));
		String name = determineName(attributes, beanDefinition);
		builder.addPropertyValue("name", name);
		builder.addPropertyValue("servletNames", attributes.get("servletNames"));
		builder.addPropertyValue("urlPatterns", extractUrlPatterns(attributes));
		registry.registerBeanDefinition(name, builder.getBeanDefinition());
	}

	private EnumSet<DispatcherType> extractDispatcherTypes(
			Map<String, Object> attributes) {
		DispatcherType[] dispatcherTypes = (DispatcherType[]) attributes
				.get("dispatcherTypes");
		if (dispatcherTypes.length == 0) {
			return EnumSet.noneOf(DispatcherType.class);
		}
		if (dispatcherTypes.length == 1) {
			return EnumSet.of(dispatcherTypes[0]);
		}
		return EnumSet.of(dispatcherTypes[0],
				Arrays.copyOfRange(dispatcherTypes, 1, dispatcherTypes.length));
	}

	private String determineName(Map<String, Object> attributes,
			BeanDefinition beanDefinition) {
		return (String) (StringUtils.hasText((String) attributes.get("filterName"))
				? attributes.get("filterName") : beanDefinition.getBeanClassName());
	}

}

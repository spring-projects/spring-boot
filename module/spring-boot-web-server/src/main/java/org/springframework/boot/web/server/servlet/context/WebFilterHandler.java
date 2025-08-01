/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.server.servlet.context;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.annotation.WebFilter;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Handler for {@link WebFilter @WebFilter}-annotated classes.
 *
 * @author Andy Wilkinson
 */
class WebFilterHandler extends ServletComponentHandler {

	WebFilterHandler() {
		super(WebFilter.class);
	}

	@Override
	public void doHandle(Map<String, @Nullable Object> attributes, AnnotatedBeanDefinition beanDefinition,
			BeanDefinitionRegistry registry) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(FilterRegistrationBean.class);
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

	private EnumSet<DispatcherType> extractDispatcherTypes(Map<String, @Nullable Object> attributes) {
		DispatcherType[] dispatcherTypes = (DispatcherType[]) attributes.get("dispatcherTypes");
		Assert.state(dispatcherTypes != null, "'dispatcherTypes' must not be null");
		if (dispatcherTypes.length == 0) {
			return EnumSet.noneOf(DispatcherType.class);
		}
		if (dispatcherTypes.length == 1) {
			return EnumSet.of(dispatcherTypes[0]);
		}
		return EnumSet.of(dispatcherTypes[0], Arrays.copyOfRange(dispatcherTypes, 1, dispatcherTypes.length));
	}

	private String determineName(Map<String, @Nullable Object> attributes, BeanDefinition beanDefinition) {
		String filterName = (String) attributes.get("filterName");
		return (StringUtils.hasText(filterName) ? filterName : getBeanClassName(beanDefinition));
	}

	private String getBeanClassName(BeanDefinition beanDefinition) {
		String name = beanDefinition.getBeanClassName();
		Assert.state(name != null, "'name' must not be null");
		return name;
	}

}

/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Map;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.StringUtils;

/**
 * Handler for {@link WebServlet @WebServlet}-annotated classes.
 *
 * @author Andy Wilkinson
 */
class WebServletHandler extends ServletComponentHandler {

	/**
	 * Constructs a new WebServletHandler object. This constructor calls the superclass
	 * constructor with the parameter WebServlet.class.
	 */
	WebServletHandler() {
		super(WebServlet.class);
	}

	/**
	 * Handles the registration of a servlet bean definition in the bean definition
	 * registry.
	 * @param attributes the attributes of the servlet registration
	 * @param beanDefinition the annotated bean definition of the servlet
	 * @param registry the bean definition registry
	 */
	@Override
	public void doHandle(Map<String, Object> attributes, AnnotatedBeanDefinition beanDefinition,
			BeanDefinitionRegistry registry) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ServletRegistrationBean.class);
		builder.addPropertyValue("asyncSupported", attributes.get("asyncSupported"));
		builder.addPropertyValue("initParameters", extractInitParameters(attributes));
		builder.addPropertyValue("loadOnStartup", attributes.get("loadOnStartup"));
		String name = determineName(attributes, beanDefinition);
		builder.addPropertyValue("name", name);
		builder.addPropertyValue("servlet", beanDefinition);
		builder.addPropertyValue("urlMappings", extractUrlPatterns(attributes));
		builder.addPropertyValue("multipartConfig", determineMultipartConfig(beanDefinition));
		registry.registerBeanDefinition(name, builder.getBeanDefinition());
	}

	/**
	 * Determines the name for a servlet handler based on the given attributes and bean
	 * definition. If the "name" attribute is present and has text, it is used as the
	 * name. Otherwise, the bean class name is used as the name.
	 * @param attributes the attributes for the servlet handler
	 * @param beanDefinition the bean definition for the servlet handler
	 * @return the determined name for the servlet handler
	 */
	private String determineName(Map<String, Object> attributes, BeanDefinition beanDefinition) {
		return (String) (StringUtils.hasText((String) attributes.get("name")) ? attributes.get("name")
				: beanDefinition.getBeanClassName());
	}

	/**
	 * Determines the MultipartConfig for the given bean definition.
	 * @param beanDefinition The annotated bean definition to determine the
	 * MultipartConfig for.
	 * @return The BeanDefinition representing the MultipartConfig, or null if no
	 * MultipartConfig annotation is present.
	 */
	private BeanDefinition determineMultipartConfig(AnnotatedBeanDefinition beanDefinition) {
		Map<String, Object> attributes = beanDefinition.getMetadata()
			.getAnnotationAttributes(MultipartConfig.class.getName());
		if (attributes == null) {
			return null;
		}
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(MultipartConfigElement.class);
		builder.addConstructorArgValue(attributes.get("location"));
		builder.addConstructorArgValue(attributes.get("maxFileSize"));
		builder.addConstructorArgValue(attributes.get("maxRequestSize"));
		builder.addConstructorArgValue(attributes.get("fileSizeThreshold"));
		return builder.getBeanDefinition();
	}

}

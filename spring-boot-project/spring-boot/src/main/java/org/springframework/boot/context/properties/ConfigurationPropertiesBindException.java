/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.properties;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.ClassUtils;

/**
 * Exception thrown when {@link ConfigurationProperties @ConfigurationProperties} binding
 * fails.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class ConfigurationPropertiesBindException extends BeanCreationException {

	private final Class<?> beanType;

	private final ConfigurationProperties annotation;

	ConfigurationPropertiesBindException(String beanName, Object bean,
			ConfigurationProperties annotation, Exception cause) {
		super(beanName, getMessage(bean, annotation), cause);
		this.beanType = bean.getClass();
		this.annotation = annotation;
	}

	/**
	 * Return the bean type that was being bound.
	 * @return the bean type
	 */
	public Class<?> getBeanType() {
		return this.beanType;
	}

	/**
	 * Return the configuration properties annotation that triggered the binding.
	 * @return the configuration properties annotation
	 */
	public ConfigurationProperties getAnnotation() {
		return this.annotation;
	}

	private static String getMessage(Object bean, ConfigurationProperties annotation) {
		StringBuilder message = new StringBuilder();
		message.append("Could not bind properties to '"
				+ ClassUtils.getShortName(bean.getClass()) + "' : ");
		message.append("prefix=").append(annotation.prefix());
		message.append(", ignoreInvalidFields=").append(annotation.ignoreInvalidFields());
		message.append(", ignoreUnknownFields=").append(annotation.ignoreUnknownFields());
		return message.toString();
	}

}

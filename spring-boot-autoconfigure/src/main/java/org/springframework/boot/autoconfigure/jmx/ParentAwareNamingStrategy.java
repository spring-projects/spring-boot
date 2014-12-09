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

package org.springframework.boot.autoconfigure.jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.util.ObjectUtils;

/**
 * JMX {@link ObjectNamingStrategy} that takes into consideration the parent
 * {@link ApplicationContext}.
 *
 * @author Dave Syer
 * @since 1.1.1
 */
public class ParentAwareNamingStrategy extends MetadataNamingStrategy implements
		ApplicationContextAware {

	private ApplicationContext applicationContext;

	private boolean ensureUniqueRuntimeObjectNames = false;

	public ParentAwareNamingStrategy(JmxAttributeSource attributeSource) {
		super(attributeSource);
	}

	/**
	 * @param ensureUniqueRuntimeObjectNames the ensureUniqueRuntimeObjectNames to set
	 */
	public void setEnsureUniqueRuntimeObjectNames(boolean ensureUniqueRuntimeObjectNames) {
		this.ensureUniqueRuntimeObjectNames = ensureUniqueRuntimeObjectNames;
	}

	@Override
	public ObjectName getObjectName(Object managedBean, String beanKey)
			throws MalformedObjectNameException {
		StringBuilder builder = new StringBuilder(beanKey);
		if (parentContextContainsSameBean(this.applicationContext, beanKey)) {
			builder.append(",context="
					+ ObjectUtils.getIdentityHexString(this.applicationContext));
		}
		if (this.ensureUniqueRuntimeObjectNames) {
			builder.append(",identity=" + ObjectUtils.getIdentityHexString(managedBean));
		}
		ObjectName name = super.getObjectName(managedBean, beanKey);
		return name;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	private boolean parentContextContainsSameBean(ApplicationContext applicationContext,
			String beanKey) {
		if (applicationContext.getParent() != null) {
			try {
				this.applicationContext.getParent().getBean(beanKey);
				return true;
			}
			catch (BeansException ex) {
				return parentContextContainsSameBean(applicationContext.getParent(),
						beanKey);
			}
		}
		return false;
	}

}

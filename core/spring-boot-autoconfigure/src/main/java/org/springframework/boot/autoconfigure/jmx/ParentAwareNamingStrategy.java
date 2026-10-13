/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.jmx;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.support.JmxUtils;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Extension of {@link MetadataNamingStrategy} that supports a parent
 * {@link ApplicationContext}.
 *
 * @author Dave Syer
 * @since 1.1.1
 */
public class ParentAwareNamingStrategy extends MetadataNamingStrategy implements ApplicationContextAware {

	@SuppressWarnings("NullAway.Init")
	private ApplicationContext applicationContext;

	private boolean ensureUniqueRuntimeObjectNames;

	public ParentAwareNamingStrategy(JmxAttributeSource attributeSource) {
		super(attributeSource);
	}

	/**
	 * Set if unique runtime object names should be ensured.
	 * @param ensureUniqueRuntimeObjectNames {@code true} if unique names should be
	 * ensured.
	 */
	public void setEnsureUniqueRuntimeObjectNames(boolean ensureUniqueRuntimeObjectNames) {
		this.ensureUniqueRuntimeObjectNames = ensureUniqueRuntimeObjectNames;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public ObjectName getObjectName(Object managedBean, @Nullable String beanKey) throws MalformedObjectNameException {
		ObjectName name = super.getObjectName(managedBean, beanKey);
		if (this.ensureUniqueRuntimeObjectNames) {
			return JmxUtils.appendIdentityToObjectName(name, managedBean);
		}
		if (parentContextContainsSameBean(this.applicationContext, beanKey)) {
			return appendToObjectName(name, "context", ObjectUtils.getIdentityHexString(this.applicationContext));
		}
		return name;
	}

	private boolean parentContextContainsSameBean(ApplicationContext context, @Nullable String beanKey) {
		if (context.getParent() == null) {
			return false;
		}
		try {
			ApplicationContext parent = this.applicationContext.getParent();
			Assert.state(parent != null, "'parent' must not be null");
			Assert.state(beanKey != null, "'beanKey' must not be null");
			parent.getBean(beanKey);
			return true;
		}
		catch (BeansException ex) {
			return parentContextContainsSameBean(context.getParent(), beanKey);
		}
	}

	private ObjectName appendToObjectName(ObjectName name, String key, String value)
			throws MalformedObjectNameException {
		Hashtable<String, String> keyProperties = name.getKeyPropertyList();
		keyProperties.put(key, value);
		return ObjectNameManager.getInstance(name.getDomain(), keyProperties);
	}

}

/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition.scan;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;

class ScanFactoryBean implements FactoryBean<ScanBean> {

	ScanFactoryBean(String value) {
		Assert.state(!value.contains("$"), "value should not contain '$'");
	}

	@Override
	public ScanBean getObject() {
		return new ScanBean("fromFactory");
	}

	@Override
	public Class<?> getObjectType() {
		return ScanBean.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

}

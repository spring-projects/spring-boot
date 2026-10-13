/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.constructorbinding.primaryconstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("my")
public class MyProperties {

	// @fold:on // fields...
	final MyBean myBean;

	private String name;

	// @fold:off

	@Autowired
	public MyProperties(MyBean myBean) {
		this.myBean = myBean;
	}

	// @fold:on // getters / setters...

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	// @fold:off

}

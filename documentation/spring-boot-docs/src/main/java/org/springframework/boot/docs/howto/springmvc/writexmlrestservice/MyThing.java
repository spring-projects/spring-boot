/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.springmvc.writexmlrestservice;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MyThing {

	private String name;

	// @fold:on // getters/setters ...
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
	// @fold:off

}

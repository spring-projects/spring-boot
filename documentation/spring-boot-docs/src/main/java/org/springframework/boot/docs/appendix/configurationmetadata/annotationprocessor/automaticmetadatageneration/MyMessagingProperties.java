/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.appendix.configurationmetadata.annotationprocessor.automaticmetadatageneration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("my.messaging")
public class MyMessagingProperties {

	private List<String> addresses = new ArrayList<>(Arrays.asList("a", "b"));

	private ContainerType containerType = ContainerType.SIMPLE;

	// @fold:on // getters/setters ...
	public List<String> getAddresses() {
		return this.addresses;
	}

	public void setAddresses(List<String> addresses) {
		this.addresses = addresses;
	}

	public ContainerType getContainerType() {
		return this.containerType;
	}

	public void setContainerType(ContainerType containerType) {
		this.containerType = containerType;
	}
	// @fold:off

	public enum ContainerType {

		SIMPLE, DIRECT

	}

}

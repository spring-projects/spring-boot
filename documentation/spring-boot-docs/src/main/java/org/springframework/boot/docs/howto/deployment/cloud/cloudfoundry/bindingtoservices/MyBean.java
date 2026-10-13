/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.deployment.cloud.cloudfoundry.bindingtoservices;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class MyBean implements EnvironmentAware {

	@SuppressWarnings("unused")
	private String instanceId;

	@Override
	public void setEnvironment(Environment environment) {
		this.instanceId = environment.getProperty("vcap.application.instance_id");
	}

	// ...

}

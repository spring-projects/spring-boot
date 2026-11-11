/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.deployment.cloud.cloudfoundry.bindingtoservices

import org.springframework.context.EnvironmentAware
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class MyBean : EnvironmentAware {

	private var instanceId: String? = null

	override fun setEnvironment(environment: Environment) {
		instanceId = environment.getProperty("vcap.application.instance_id")
	}

	// ...

}


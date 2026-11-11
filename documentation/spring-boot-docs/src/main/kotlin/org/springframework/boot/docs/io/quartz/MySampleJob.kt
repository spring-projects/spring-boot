/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.quartz

import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean

class MySampleJob : QuartzJobBean() {

	// @fold:on // fields ...
	private var myService: MyService? = null

	private var name: String? = null
	// @fold:off

	// Inject "MyService" bean
	fun setMyService(myService: MyService?) {
		this.myService = myService
	}

	// Inject the "name" job data property
	fun setName(name: String?) {
		this.name = name
	}

	override fun executeInternal(context: JobExecutionContext) {
		myService!!.someMethod(context.fireTime, name)
	}

}


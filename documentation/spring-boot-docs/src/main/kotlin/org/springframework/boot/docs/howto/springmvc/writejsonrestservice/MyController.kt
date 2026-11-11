/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.springmvc.writejsonrestservice

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MyController {

	@RequestMapping("/thing")
	fun thing(): MyThing {
		return MyThing()
	}

}


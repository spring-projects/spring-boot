/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.springmvc.writejsonrestservice;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyController {

	@RequestMapping("/thing")
	public MyThing thing() {
		return new MyThing();
	}

}

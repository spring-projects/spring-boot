package org.springframework.boot.controller;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class DemoController {

	@RequestMapping("/hello")
	public String hello() {
		return "hello world";
	}

}

package org.test

@Grab("spring-boot-starter-shell-remote")

@RestController
class SampleController {

	@RequestMapping("/")
	public def hello() {
		[message: "Hello World"]
	}
}



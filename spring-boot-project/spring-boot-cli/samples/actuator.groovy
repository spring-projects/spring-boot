package org.test

@Grab("spring-boot-starter-actuator")

@RestController
class SampleController {

	@RequestMapping("/")
	public def hello() {
		[message: "Hello World!"]
	}
}

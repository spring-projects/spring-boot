package org.test

@Grab("org.springframework.zero:spring-zero-actuator:0.5.0.BUILD-SNAPSHOT")

@Controller
class SampleController {

	@RequestMapping("/")
	@ResponseBody
	public def hello() {
		[message: "Hello World!"]
	}
}



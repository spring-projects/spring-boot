package org.test

@Grab("spring-boot-starter-actuator")

@Controller
class SampleController {

	@RequestMapping("/")
	@ResponseBody
	public def hello() {
		[message: "Hello World!"]
	}
}



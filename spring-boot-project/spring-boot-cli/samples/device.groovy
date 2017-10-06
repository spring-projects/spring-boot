package org.test

@Controller
@EnableDeviceResolver
class Example {

	@RequestMapping("/")
	@ResponseBody
	String helloWorld(Device device) {
		if (device.isNormal()) {
			"Hello Normal Device!"
		} else if (device.isMobile()) {
			"Hello Mobile Device!"
		} else if (device.isTablet()) {
			"Hello Tablet Device!"
		} else {
			"Hello Unknown Device!"
		}
	}

}

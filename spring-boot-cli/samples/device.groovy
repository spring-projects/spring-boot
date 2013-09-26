package org.test

@Controller
@EnableDeviceResolver
class Example {

	@RequestMapping("/")
	@ResponseBody
	public String helloWorld(Device device) {
		if (device.isNormal()) {
			return "Hello Normal Device!"
		} else if (device.isMobile()) {
			return "Hello Mobile Device!"
		} else if (device.isTablet()) {
			return "Hello Tablet Device!"
		} else {
			return "Hello Unknown Device!"
		}
	}

}

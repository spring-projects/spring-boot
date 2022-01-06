package app

@Grab("thymeleaf-spring6")
@Controller
class Example {

	@RequestMapping("/")
	public String helloWorld(Map<String,Object> model) {
		model.putAll([title: "My Page", date: new Date(), message: "Hello World"])
		return "home"
	}
}

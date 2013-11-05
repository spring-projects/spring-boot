@Grab("org.codehaus.groovy:groovy-ant:2.1.6")

@RestController
class MainController {

	@RequestMapping("/")
	def home() {
		new AntBuilder().echo(message:"Hello world")
		[message: "Hello World"]
	}
}

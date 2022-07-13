@Grab("org.apache.groovy:groovy-ant:4.0.1")
import groovy.ant.AntBuilder

@RestController
class MainController {

	@RequestMapping("/")
	def home() {
		new AntBuilder().echo(message:"Hello world")
		[message: "Hello World"]
	}
}

@Controller
class App {
	@RequestMapping("/")
	@ResponseBody
	String home() {
	  return "Hello World!"
	}
}
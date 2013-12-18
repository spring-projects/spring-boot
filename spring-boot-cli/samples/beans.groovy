@RestController
class Application {
	@Autowired
	String foo
	@RequestMapping("/")
	String home() {
		"Hello ${foo}!"
	}
}

beans {
  foo String, "World"
}
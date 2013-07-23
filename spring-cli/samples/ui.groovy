package app

@Grab("org.thymeleaf:thymeleaf-spring3:2.0.16")
@Controller
class Example {

	@RequestMapping("/")
	public String helloWorld(Map<String,Object> model) {
		model.putAll([title: "My Page", date: new Date(), message: "Hello World"])
		return "home";
	}

}

@Configuration
@Log
class MvcConfiguration extends WebMvcConfigurerAdapter {

  @Override
  void addInterceptors(def registry) { 
    log.info("Registering temporary file interceptor")
    registry.addInterceptor(temporaryFileInterceptor())
  }

  @Bean
  HandlerInterceptor temporaryFileInterceptor() {
    log.info("Creating temporary file interceptor")
    new HandlerInterceptorAdapter() {
      @Override
      postHandle(def request, def response, def handler, ModelAndView mav) { 
        log.info("Model: " + model)
      }
    }
  }
}
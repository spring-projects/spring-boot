package app

@Grab("thymeleaf-spring4")
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
	void addInterceptors(InterceptorRegistry registry) {
		log.info "Registering interceptor"
		registry.addInterceptor(interceptor())
	}

	@Bean
	HandlerInterceptor interceptor() {
		log.info "Creating interceptor"
		[
			postHandle: { request, response, handler, mav ->
				log.info "Intercepted: model=" + mav.model
			}
		] as HandlerInterceptorAdapter
	}
}
package app

import groovy.util.logging.Log

@Grab("org.thymeleaf:thymeleaf-spring3:2.0.16")
@Controller
class Example {

	@RequestMapping("/")
	public String helloWorld(Map<String,Object> model) {
		model.putAll([title: "My Page", date: new Date(), message: "Hello World"])
		return "home";
	}

}

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
@Log
class MvcConfiguration extends WebMvcConfigurerAdapter {

  @Override
  void addInterceptors(InterceptorRegistry registry) { 
    log.info("Registering interceptor")
    registry.addInterceptor(interceptor())
  }

  @Bean
  HandlerInterceptor interceptor() {
    log.info("Creating interceptor")
    new HandlerInterceptorAdapter() {
      @Override
      void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mav) { 
        log.info("Model: " + mav.model)
      }
    }
  }
}
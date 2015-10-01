package commands

import org.springframework.boot.actuate.endpoint.BeansEndpoint

class beans {

	@Usage("Display beans in ApplicationContext")
	@Command
	def main(InvocationContext context) {
		def result = [:]
		context.attributes['spring.beanfactory'].getBeansOfType(BeansEndpoint.class).each { name, endpoint ->
			result.put(name, endpoint.invoke())
		}
		result.size() == 1 ? result.values()[0] : result
	}

}
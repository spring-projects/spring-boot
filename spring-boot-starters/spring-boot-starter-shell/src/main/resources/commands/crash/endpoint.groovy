package commands

import org.springframework.boot.actuate.endpoint.Endpoint;

@Usage("Invoke actuator endpoints")
class endpoint {

	@Usage("List all available and enabled actuator endpoints")
	@Command
	def list(InvocationContext context) {
		
		context.attributes['spring.beanfactory'].getBeansOfType(Endpoint.class).each { name, endpoint ->
			if (endpoint.isEnabled()) {
				out.println name
			}
		}
		""
	}

	@Usage("Invoke provided actuator endpoint")
	@Command
	def invoke(InvocationContext context, @Usage("The object name pattern") @Required @Argument String name) {
		
		context.attributes['spring.beanfactory'].getBeansOfType(Endpoint.class).each { n, endpoint ->
			if (n.equals(name) && endpoint.isEnabled()) {
				out.println endpoint.invoke()
			}
		}
		""
	}
	

}
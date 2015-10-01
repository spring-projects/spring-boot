package commands

import org.springframework.boot.actuate.endpoint.AutoConfigurationReportEndpoint

class autoconfig {

	@Usage("Display auto configuration report from ApplicationContext")
	@Command
	void main(InvocationContext context) {
		context.attributes['spring.beanfactory'].getBeansOfType(AutoConfigurationReportEndpoint.class).each { name, endpoint ->
			def report = endpoint.invoke()
			out.println "Endpoint: " + name + "\n\nPositive Matches:\n================\n"
			report.positiveMatches.each { key, list ->
				out.println key + ":"
				list.each { mandc ->
					out.println "  " + mandc.condition + ": " + mandc.message
				}
			}
			out.println "\nNegative Matches\n================\n"
			report.negativeMatches.each { key, list ->
				out.println key + ":"
				list.each { mandc ->
					out.println "  " + mandc.condition + ": " + mandc.message
				}
			}
		}
	}

}
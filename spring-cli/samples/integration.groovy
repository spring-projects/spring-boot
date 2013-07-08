package org.test

@Component
@EnableIntegrationPatterns
class SpringIntegrationExample implements CommandLineRunner {

	@Bean
	MessageFlow flow(ApplicationContext context) {
		def builder = new IntegrationBuilder(context)
		builder.messageFlow { transform {"Hello, $it!"} }
	}

	@Override
	void run(String... args) {
		print flow().sendAndReceive("World")
	}
}

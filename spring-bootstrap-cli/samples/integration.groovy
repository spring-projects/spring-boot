package org.test

@Component
class SpringIntegrationExample implements CommandLineRunner {

	def builder = new IntegrationBuilder()
	def flow = builder.messageFlow {
		transform {"Hello, $it!"}
	}

    @Override
	public void run(String... args) {
      print flow.sendAndReceive("World")
	}
}

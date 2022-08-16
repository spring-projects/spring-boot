@Grab("org.apache.camel:camel-core:3.18.0")

import org.apache.camel.builder.RouteBuilder;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

class CammelRunner {

    RouteBuilder hello() {
        return new RouteBuilder()  {
            public void configure() {
                from("timer:simple?period=1000")
                .log("Hello Cameleers");
            }
        };
    }
    

    @Bean
	public CommandLineRunner runner() {
		return (args) -> {
            try (CamelContext camelContext = new DefaultCamelContext()) {
                camelContext.addRoutes(hello());
                camelContext.start();
                Thread.sleep(10000);
            }
		};
	}
}

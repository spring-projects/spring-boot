package commands

import org.crsh.text.ui.UIBuilder
import org.springframework.boot.actuate.endpoint.MetricsEndpoint

class metrics {

	@Usage("Display metrics provided by Spring Boot")
	@Command
	public void main(InvocationContext context) {

		context.takeAlternateBuffer();
		try {
			while (!Thread.interrupted()) {
				out.cls()
				out.show(new UIBuilder().table(columns:[1]) {
					header {
						table(columns:[1], separator: dashed) {
							header(bold: true, fg: black, bg: white) { label("metrics"); }
						}
					}
					row {
						table(columns:[1, 1]) {
							header(bold: true, fg: black, bg: white) {
								label("NAME")
								label("VALUE")
							}

							context.attributes['spring.beanfactory'].getBeansOfType(MetricsEndpoint.class).each { name, metrics ->
								metrics.invoke().each { k, v ->
									row {
										label(k)
										label(v)
									}
								}
							}
						}
					}
				}
				);
				out.flush();
				Thread.sleep(1000);
			}
		}
		finally {
			context.releaseAlternateBuffer();
		}
	}
}
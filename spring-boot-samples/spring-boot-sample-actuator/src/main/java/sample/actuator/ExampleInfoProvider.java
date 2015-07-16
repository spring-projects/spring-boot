package sample.actuator;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoProvider;
import org.springframework.stereotype.Component;

@Component
public class ExampleInfoProvider implements InfoProvider {

	@Override
	public Info provide() {
		Info result = new Info();
		result.put("somekey", "somevalue");
		return result;
	}

	@Override
	public String name() {
		return "example";
	}
}

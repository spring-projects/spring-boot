package sample.actuator;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.info.InfoProvider;
import org.springframework.stereotype.Component;

@Component
public class ExampleInfoProvider implements InfoProvider {

	@Override
	public Map<String, Object> provide() {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("somekey", "somevalue");
		return result;
	}

}

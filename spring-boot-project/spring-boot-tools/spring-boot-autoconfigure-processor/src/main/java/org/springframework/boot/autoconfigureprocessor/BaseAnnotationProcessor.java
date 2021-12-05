package org.springframework.boot.autoconfigureprocessor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BaseAnnotationProcessor {

	public static void writeProperties(
			Map<String, String> properties,
			OutputStream outputStream
	) throws IOException {
		if (properties.isEmpty()) {
			return;
		}
		try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
			for (Map.Entry<String, String> entry : properties.entrySet()) {
				writer.append(entry.getKey());
				writer.append("=");
				writer.append(entry.getValue());
				writer.append(System.lineSeparator());
			}
		}
	}
}

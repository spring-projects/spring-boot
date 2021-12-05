package org.springframework.boot.autoconfigureprocessor.extractor;

import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSName;

import javax.lang.model.element.AnnotationMirror;
import java.util.*;

public class OnBeanConditionValueExtractor extends AbstractValueExtractor {

	@Override
	public List<Object> getValues(Object annotation) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		if (annotation instanceof AnnotationMirror) {
			((AnnotationMirror) annotation).getElementValues()
					.forEach((key, value) -> attributes.put(key.getSimpleName().toString(), value));
		}
		if (annotation instanceof KSAnnotation) {
			((KSAnnotation) annotation).getArguments()
					.forEach((key) -> {
						KSName kName = key.getName();
						if (kName != null) {
							attributes.put(kName.getShortName(), key);
						}
					});
		}

		// TODO add check for kotlin
		Object kek = attributes.get("name");
		if (kek != null) {
			return Collections.emptyList();
		}

		List<Object> result = new ArrayList<>();
		extractValues(attributes.get("value")).forEach(result::add);
		extractValues(attributes.get("type")).forEach(result::add);
		return result;
	}
}

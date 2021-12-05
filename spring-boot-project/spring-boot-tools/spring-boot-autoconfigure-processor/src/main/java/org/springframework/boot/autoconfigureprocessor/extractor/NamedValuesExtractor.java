package org.springframework.boot.autoconfigureprocessor.extractor;

import com.google.devtools.ksp.symbol.KSAnnotation;

import javax.lang.model.element.AnnotationMirror;
import java.util.*;

public class NamedValuesExtractor extends AbstractValueExtractor {


	private final Set<String> names;

	public NamedValuesExtractor(String... names) {
		this.names = new HashSet<>(Arrays.asList(names));
	}

	@Override
	public List<Object> getValues(Object annotation) {
		if (annotation instanceof AnnotationMirror) {
			return getValues2((AnnotationMirror) annotation);
		}
		if (annotation instanceof KSAnnotation) {
			return getValues2((KSAnnotation) annotation);
		}
		return Collections.emptyList();
	}

	private List<Object> getValues2(AnnotationMirror annotation) {
		List<Object> result = new ArrayList<>();
		annotation.getElementValues().forEach((key, value) -> {
			if (this.names.contains(key.getSimpleName().toString())) {
				extractValues(value).forEach(result::add);
			}
		});
		return result;
	}

	private List<Object> getValues2(KSAnnotation annotation) {
		List<Object> result = new ArrayList<>();
		annotation.getArguments().forEach((arg) -> {
			String argName;
			if (arg.getName() != null) {
				argName = arg.getName().getShortName();
			} else {
				argName = null;
			}

			if (this.names.contains(argName)) {
				extractValues(arg.getValue()).forEach(result::add);
			}
		});
		return result;
	}
}

package org.springframework.boot.autoconfigureprocessor.extractor;

import com.google.devtools.ksp.symbol.KSName;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSValueArgument;
import org.springframework.boot.autoconfigureprocessor.Elements;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.type.DeclaredType;
import java.util.List;
import java.util.stream.Stream;

abstract class AbstractValueExtractor implements ValueExtractor {

	Object getValue(Object annotation) {
		if (annotation instanceof AnnotationValue) {
			return ((AnnotationValue) annotation).getValue();
		}
		if (annotation instanceof KSValueArgument) {
			return ((KSValueArgument) annotation).getValue();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	protected Stream<Object> extractValues(Object annotationValue) {
		if (annotationValue == null) {
			return Stream.empty();
		}
		Object value = getValue(annotationValue);
		if (value instanceof List) {
			return ((List<AnnotationValue>) value).stream()
					.map((annotation) -> extractValue(annotation.getValue()));
		}
		return Stream.of(extractValue(value));
	}

	private Object extractValue(Object value) {
		if (value instanceof DeclaredType) {
			return Elements.getQualifiedName(((DeclaredType) value).asElement());
		}
		if (value instanceof KSType) {
			KSName name = ((KSType) value).getDeclaration().getQualifiedName();
			if (name != null) {
				return name.asString();
			} else {
				return null;
			}
		}
		return value;
	}


}

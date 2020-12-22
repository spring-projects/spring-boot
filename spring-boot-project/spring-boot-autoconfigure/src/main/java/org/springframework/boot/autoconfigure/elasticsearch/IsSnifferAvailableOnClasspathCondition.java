package org.springframework.boot.autoconfigure.elasticsearch;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class IsSnifferAvailableOnClasspathCondition implements Condition {
	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		return isSnifferJarOnClasspath();
	}
	private boolean isSnifferJarOnClasspath()
	{
		try {
			Class.forName("org.elasticsearch.client.sniff.Sniffer");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}

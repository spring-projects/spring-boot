package org.springframework.boot.autoconfigure.condition;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnCore extends SpringBootCondition {
	private static final int AVALIABLE_PROCESSORS =   Runtime.getRuntime().availableProcessors();

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(ConditionalOnCores.class.getName());
		int cores = (int) attributes.get("value");
		Operators operator = (Operators) attributes.get("operator");


		boolean match = cores==AVALIABLE_PROCESSORS;
				
		if(Operators.EQ.equals(operator)){
			match = cores==AVALIABLE_PROCESSORS;
		} else if(Operators.GE.equals(operator)){
			match = cores<=AVALIABLE_PROCESSORS;
		} else if(Operators.LE.equals(operator)){
			match = cores>=AVALIABLE_PROCESSORS;
		} else if(Operators.GT.equals(operator)){
			match = cores<AVALIABLE_PROCESSORS;
		} else if(Operators.LT.equals(operator)){
			match = cores>AVALIABLE_PROCESSORS;
		}
		return new ConditionOutcome(match, getMessage(cores,operator, cores));
	}
	
	private String getMessage(int cores,Operators operator,  int coresAvaliable) {
		return "Required " + cores + "  "+ operator + "  "+  coresAvaliable;
	}

}

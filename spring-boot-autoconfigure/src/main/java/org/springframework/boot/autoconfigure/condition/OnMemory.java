package org.springframework.boot.autoconfigure.condition;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnMemory extends SpringBootCondition {
	private static final Long MEMORY_AVALIABLE =  Runtime.getRuntime().totalMemory();

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(ConditionalOnMemory.class.getName());
		long memory = (long) attributes.get("value");
		Operators operator = (Operators) attributes.get("operator");


		boolean match = memory==MEMORY_AVALIABLE;
		
		if(Operators.EQ.equals(operator)){
			match = memory==MEMORY_AVALIABLE;
		} else if(Operators.GE.equals(operator)){
			match = memory<=MEMORY_AVALIABLE;
		} else if(Operators.LE.equals(operator)){
			match = memory>=MEMORY_AVALIABLE;
		} else if(Operators.GT.equals(operator)){
			match = memory<MEMORY_AVALIABLE;
		} else if(Operators.LT.equals(operator)){
			match = memory>MEMORY_AVALIABLE;
		}
		
		return new ConditionOutcome(match, getMessage(memory, operator, MEMORY_AVALIABLE));
	}
	
	private String getMessage(Long memory,Operators operator, Long memoryAvaliable) {
		return "Required " + memory + "  "+ operator + "  "+ memoryAvaliable;
	}

}

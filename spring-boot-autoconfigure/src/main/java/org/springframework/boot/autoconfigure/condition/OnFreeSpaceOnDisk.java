package org.springframework.boot.autoconfigure.condition;

import java.io.File;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnFreeSpaceOnDisk extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(ConditionalOnFreeSpaceOnDisk.class.getName());
		long freeSpace = (long) attributes.get("value");
		String path = (String) attributes.get("path");
		Operators operator = (Operators) attributes.get("operator");

		boolean match = false;
		
		File[] files = File.listRoots();
		for(File file : files){
			if(file.getPath().equals(path) ){
				
				long freeSpaceInDisk = file.getFreeSpace();
				
				if(Operators.EQ.equals(operator)){
					match = freeSpace==freeSpaceInDisk;
				} else if(Operators.GE.equals(operator)){
					match = freeSpace<=freeSpaceInDisk;
				} else if(Operators.LE.equals(operator)){
					match = freeSpace>=freeSpaceInDisk;
				} else if(Operators.GT.equals(operator)){
					match = freeSpace<freeSpaceInDisk;
				} else if(Operators.LT.equals(operator)){
					match = freeSpace>freeSpaceInDisk;
				}
			}
		}
		
		return new ConditionOutcome(match, getMessage(path, freeSpace));
	}
	
	private String getMessage(String path, long freeSpace) {
		return "Required " + path + " found " + freeSpace;
	}

}

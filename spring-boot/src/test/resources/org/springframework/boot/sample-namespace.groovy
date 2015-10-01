import org.springframework.boot.sampleconfig.MyComponent;

beans {
	xmlns([ctx:'http://www.springframework.org/schema/context'])
	ctx.'component-scan'('base-package':'nonexistent')
	myGroovyComponent(MyComponent) {}
}
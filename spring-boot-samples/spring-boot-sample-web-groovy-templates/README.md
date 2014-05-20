This sample application uses Spring Boot and
[Groovy templates](http://beta.groovy-lang.org/docs/groovy-2.3.0/html/documentation/markup-template-engine.html)
in the View layer. The templates for this app live in
`classpath:/templates/`, which is the conventional location for Spring
Boot. External configuration is available via
"spring.groovy.template.*".

The templates all use a fixed "layout" implemented anticipating
changes in Groovy 2.3.1 using a custom `BaseTemplate` class
(`LayoutAwareTemplate`).

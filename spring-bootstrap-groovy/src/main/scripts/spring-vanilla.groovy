// Spring stuff needs to be on the system classloader apparently (when using @Configuration)
@GrabResolver(name='spring-milestone', root='http://maven.springframework.org/milestone')
@GrabConfig(systemClassLoader=true)
@Grab("org.springframework:spring-context:4.0.0.BOOTSTRAP-SNAPSHOT")
@GrabExclude("commons-logging:commons-logging")
@Grab("org.slf4j:jcl-over-slf4j:1.6.1")
@Grab("org.slf4j:slf4j-jdk14:1.6.1")
import org.springframework.context.annotation.AnnotationConfigApplicationContext

// Now create a Spring context
AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()
// Get the args and turn them into classes
def configs = []
def parameters = []
boolean endconfigs = false
args.each { arg ->
            if (arg.endsWith(".class")) {
                configs += arg.replaceAll(".class", "")
            } else {
                parameters += arg
            }
          }
configs = configs as Class[]
parameters = parameters as String[]
// Register the config classes, can be @Configuration or @Component etc.
ctx.register(configs)
ctx.refresh()

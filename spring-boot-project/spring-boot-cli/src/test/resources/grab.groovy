@GrabResolver(name="test", root="file:./src/test/plugins")
@Grab("custom:custom:0.0.1")
@Controller
class Foo {}

println "Hello Grab"
// Get the args and turn them into classes
def configs = []
def parameters = []
args.each { arg ->
  if (arg.endsWith(".class")) {
    configs << arg.replaceAll(".class", "")
  } else {
    parameters << arg
  }
}

// Dynamically grab some dependencies
def dependencySource = "org.springframework.bootstrap.grapes.Dependencies" as Class // TODO: maybe strategise this
def dependencies = [*dependencySource.defaults(), *dependencySource.dependencies(configs)]
configs = dependencies + configs

// Do this before any Spring auto stuff is used in case it enhances the classpath
configs = configs as Class[]
parameters = parameters as String[]

// Now run the application
def applicationClass = "org.springframework.bootstrap.SpringApplication" as Class

applicationClass.run(configs, parameters)

import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.ast.ClassHelper
import groovy.util.logging.Log

def bootstrap = 'org.springframework.bootstrap.grapes.BootstrapGrapes' as Class

void addImport(module, path) {
  def name = path.lastIndexOf('.').with {it != -1 ? path[it+1..<path.length()] : path}
  if (name=="*") {
    // Doesn't work?
    name = path.lastIndexOf('.').with {path[0..<it] }
    module.addStarImport(name, [])
  } else {
    module.addImport(name, ClassHelper.make(path), [])
  }
}

withConfig(configuration) {

  ast(Log)

  imports {
    normal 'javax.sql.DataSource'
    normal 'org.springframework.stereotype.Component'
    normal 'org.springframework.stereotype.Controller'
    normal 'org.springframework.stereotype.Repository'
    normal 'org.springframework.stereotype.Service'
    normal 'org.springframework.beans.factory.annotation.Autowired'
    normal 'org.springframework.beans.factory.annotation.Value'
    normal 'org.springframework.context.annotation.Import'
    normal 'org.springframework.context.annotation.ImportResource'
    normal 'org.springframework.context.annotation.Profile'
    normal 'org.springframework.context.annotation.Scope'
    normal 'org.springframework.context.annotation.Configuration'
    normal 'org.springframework.context.annotation.Bean'
    normal 'org.springframework.bootstrap.CommandLineRunner'
  }

  def dependencySource = "org.springframework.bootstrap.grapes.Dependencies" as Class // TODO: maybe strategise this

  inline(phase:'CONVERSION') { source, context, classNode ->

    def module = source.getAST()

    if (classNode.name.contains("Hadoop")) { 
      def hadoop = dependencySource.hadoop() as Class[]
      ['org.springframework.data.hadoop.mapreduce.JobRunner',
       'org.springframework.data.hadoop.mapreduce.JobFactoryBean'
      ].each { path -> addImport(module, path) }
      module.addImport("HadoopConfiguration", ClassHelper.make("org.apache.hadoop.conf.Configuration"), [])
    }

    classNode.annotations.each { 

      def name = it.classNode.name
      if (name=='Controller' || name=='EnableWebMvc') {
        def web = dependencySource.web() as Class[]
        ['org.springframework.web.bind.annotation.RequestBody', 
         'org.springframework.web.bind.annotation.RequestParam', 
         'org.springframework.web.bind.annotation.PathVariable', 
         'org.springframework.web.bind.annotation.RequestHeader', 
         'org.springframework.web.bind.annotation.RequestMethod', 
         'org.springframework.web.bind.annotation.RequestBody', 
         'org.springframework.web.bind.annotation.ResponseBody', 
         'org.springframework.web.bind.annotation.ResponseStatus', 
         'org.springframework.web.bind.annotation.RequestMapping',
         'org.springframework.web.bind.annotation.ExceptionHandler',
         'org.springframework.web.bind.annotation.ModelAttribute',
         'org.springframework.web.bind.annotation.CookieValue',
         'org.springframework.web.servlet.config.annotation.EnableWebMvc',
         'org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry',
         'org.springframework.web.servlet.config.annotation.ViewControllerRegistry',
         'org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter'].each { path -> addImport(module, path) }
      }

      if (name=='EnableBatchProcessing') {
        def batch = dependencySource.batch() as Class[]
        ['org.springframework.batch.repeat.RepeatStatus',
         'org.springframework.batch.core.scope.context.ChunkContext',
         'org.springframework.batch.core.step.tasklet.Tasklet',
         'org.springframework.batch.core.configuration.annotation.StepScope',
         'org.springframework.batch.core.configuration.annotation.JobBuilderFactory',
         'org.springframework.batch.core.configuration.annotation.StepBuilderFactory',
         'org.springframework.batch.core.configuration.annotation.EnableBatchProcessing',
         'org.springframework.batch.core.Step',
         'org.springframework.batch.core.StepExecution',
         'org.springframework.batch.core.StepContribution',
         'org.springframework.batch.core.Job',
         'org.springframework.batch.core.JobExecution',
         'org.springframework.batch.core.JobParameter',
         'org.springframework.batch.core.JobParameters',
         'org.springframework.batch.core.launch.JobLauncher',
         'org.springframework.batch.core.converter.DefaultJobParametersConverter'].each { path -> addImport(module, path) }
      }

    }

  }

}

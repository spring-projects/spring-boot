html {
  head {
    title(title)
    link(rel:'stylesheet', href:'/css/bootstrap.min.css')
  }
  body {
    div(class:'container') {
      div(class:'navbar') {
        div(class:'navbar-inner') {
          a(class:'brand',
              href:'http://groovy-lang.org/404.html') {
              yield 'Groovy - Layout'
          }
          ul(class:'nav') {
            li {
              a(href:'/') {
                yield 'Messages'
              }
            }
          }
        }
      }
      h1(title)
      div { content() }
    }
  }
}

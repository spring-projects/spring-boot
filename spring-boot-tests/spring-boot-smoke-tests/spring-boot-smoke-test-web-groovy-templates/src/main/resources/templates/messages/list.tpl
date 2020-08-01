layout 'layout.tpl', title: 'Messages : View all',
  content: contents {
    div(class:'container') {
      div(class:'pull-right') {
        a(href:'/?form', 'Create Message')
      }
      table(class:'table table-bordered table-striped') {
        thead {
          tr {
            td 'ID'
            td 'Created'
            td 'Summary'
          }
        }
        tbody {
          if (messages.empty) { tr { td(colspan:'3', 'No Messages' ) } }
          messages.each { message ->
            tr { 
              td message.id
              td "${message.created}"
              td {
                a(href:"/${message.id}") {
                  yield message.summary
                }
              }
            }
          }
        }
      }
    }
  }
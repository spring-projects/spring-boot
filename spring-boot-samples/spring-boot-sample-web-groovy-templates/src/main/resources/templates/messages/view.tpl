layout 'layout.tpl', title:'Messages : View',
  content: contents {
    div(class:'container') {
      if (globalMessage) {
        div (class:'alert alert-success', globalMessage)
      }
      div(class:'pull-right') {
        a(href:'/', 'Messages')
      }
      dl {
        dt 'ID'
        dd(id:'id', message.id)
        dt 'Date'
        dd(id:'created', "${message.created}")
        dt 'Summary'
        dd(id:'summary', message.summary)
        dt 'Message'
        dd(id:'text', message.text)
      }
    }
  }
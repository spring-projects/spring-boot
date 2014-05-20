layout 'layout.tpl', title: 'Messages : Create',
  content: contents {
    div (class:'container') {
      form (id:'messageForm', action:'/', method:'post') {
      	if (formErrors) {
          div(class:'alert alert-error') {
            formErrors.each { error ->
              p error.defaultMessage
            }
          }
        }
        div (class:'pull-right') {
          a (href:'/', 'Messages')
        }
        label (for:'summary', 'Summary')
        input (name:'summary', type:'text', value:message.summary?:'',
            class:fieldErrors?.summary ? 'field-error' : 'none')
        label (for:'text', 'Message')
        textarea (name:'text', class:fieldErrors?.text ? 'field-error' : 'none', message.text?:'')
        div (class:'form-actions') {
          input (type:'submit', value:'Create')
        }
      }
    }
  }
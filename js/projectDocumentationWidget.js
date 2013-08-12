window.Spring = window.Spring || {};

Spring.ProjectDocumentationWidget = function() {
  var projectId = $('[data-documentation-widget]').data('documentation-widget');
  var project = new Spring.Project({id: projectId });

  var promise = project.fetch();
  promise.success(function() {
    new Spring.ProjectDocumentationWidgetView({
      el: '[data-documentation-widget]', model: project
    }).render();
  });
};

Spring.Project = Backbone.Model.extend({
  urlRoot: "http://localhost:8080/projects",

  sync: function(method, model, options) {
    var params = _.extend({
      type:         'GET',
      dataType:     'jsonp',
      url:			model.url()+"?callback=?",
      processData:  false
    }, options);

    return $.ajax(params);
  }
});

Spring.ProjectDocumentationWidgetView = Backbone.View.extend({
  template: _.template(
    "<%= name %>" +
    "<ul>" +
      "<% _.each(releases, function(release) { %>" +
        "<li>" +
          "<%= release.version %>" +
          "<%= statusIcon(release.status) %>" +
          "<a href='<%= release.referenceUrl %>'>Reference</a>" +
          "<a href='<%= release.apiUrl %>'>API</a>" +
        "</li>" +
      "<% }); %>" +
    "</ul>"
  ),

  initialize: function() {
    _.bindAll(this, "render");
  },

  render: function() {
    var data = _.extend(this.model.attributes, {statusIcon: this._statusIcon});

    this.$el.html(
      this.template(data)
    );

    return this;
  },

  _statusIcon: function(status) {
    switch(status) {
      case "pre" :
        return "<i class='icon-projects-pre'></i>";
      case "current" :
        return "<i class='icon-projects-current'></i>";
      default :
        return "";
    }
  }
});
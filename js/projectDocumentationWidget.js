window.Spring = window.Spring || {};

Spring.ProjectDocumentationWidget = function() {
  var projectId = $('[data-documentation-widget]').data('documentation-widget');
  var project = new Spring.Project({id: projectId});

  var promise = project.fetch();
  promise.success(function() {
    new Spring.ProjectDocumentationWidgetView({
      el: '[data-documentation-widget]',
      model: project,
      template: $("#project-documentation-widget-template").text()
    }).render();
  });
};

Spring.Release = {
  statusIconClass: function() {
    if (this.preRelease) {
      return "icon-projects-pre";
    } else if (this.current) {
      return "icon-projects-current";
    } else {
      return "icon-projects-supported";
    }
  }
}

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
  },

  releases: function() {
    return _.map(this.get('projectReleases'), function(r) {

      return _.extend(r, Spring.Release);
    });
  },

  data: function() {
    return _.extend(this.attributes, {releases: this.releases()})
  }
});

Spring.ProjectDocumentationWidgetView = Backbone.View.extend({
  initialize: function() {
    this.template = _.template(this.options.template);
    _.bindAll(this, "render");
  },

  render: function() {
    this.$el.html(
      this.template(this.model.data())
    );
    return this;
  }

});
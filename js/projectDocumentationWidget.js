window.Spring = window.Spring || {};

Spring.ProjectDocumentationWidget = function() {
  var projectId = $('[data-documentation-widget]').data('documentation-widget');
  var projectUrl = apiBaseUrl + "/projects/" + projectId;
  var promise = Spring.loadProject(projectUrl);

  promise.then(function(project) {
    new Spring.ProjectDocumentationWidgetView({
      el: '[data-documentation-widget]',
      model: project,
      template: $("#project-documentation-widget-template").text()
    }).render();
  });
};

Spring.loadProject = function(url) {
  return $.ajax(url, {
    dataType:     'jsonp',
    processData:  false
  }).then(function(value) {
    return new Spring.Project(value);
  });
}

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

Spring.Project = function(data) {
  _.extend(this, data);
  this.releases = _.map(this.projectReleases, function(r) {
    return _.extend(r, Spring.Release);
  });

  return this;
};

Spring.ProjectDocumentationWidgetView = Backbone.View.extend({
  initialize: function() {
    this.template = _.template(this.options.template);
    _.bindAll(this, "render");
  },

  render: function() {
    this.$el.html(
      this.template(this.model)
    );
    return this;
  }

});
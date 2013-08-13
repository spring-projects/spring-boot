window.Spring = window.Spring || {};

Spring.ProjectDocumentationWidget = function() {
  var quickStartEl = $('.js-quickstart-selector');
  var documentationEl = $('.js-documentation-widget');

  var projectUrl = apiBaseUrl + "/projects/" + projectId;
  var promise = Spring.loadProject(projectUrl);

  promise.then(function(project) {
    new Spring.WidgetView({
      el: documentationEl,
      model: project,
      template: $("#project-documentation-widget-template").text()
    }).render();

    var mavenWidget = new Spring.WidgetView({
      el: $('.js-quickstart-maven-widget'),
      model: project.releases[0],
      template: $("#project-quickstart-maven-widget-template").text()
    }).render();

    new Spring.QuickStartSelectorView({
      el: quickStartEl,
      model: project,
      template: $("#project-quickstart-selector-template").text(),
      mavenWidget: mavenWidget
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

Spring.Release = function(data) {
  _.extend(this, data);
}

Spring.Release.prototype = {
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
  var self = this;
  this.releases = _.map(this.projectReleases, function(r) {
    return new Spring.Release(r);
  });

  return this;
};

Spring.WidgetView = Backbone.View.extend({
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

Spring.QuickStartSelectorView = Backbone.View.extend({
  events: {
    "change .selector": "updateMaven"
  },

  initialize: function() {
    this.template = _.template(this.options.template);
    this.mavenWidget = this.options.mavenWidget;
    _.bindAll(this, "render", "updateMaven");
  },

  render: function() {
    this.$el.html(
      this.template(this.model)
    );
    return this;
  },

  updateMaven: function() {
    this.mavenWidget.model = this.model.releases[this.$('.selector :selected').val()];
    this.mavenWidget.render();
  }

});
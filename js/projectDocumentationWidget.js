window.Spring = window.Spring || {};

Spring.ProjectDocumentationWidget = function() {
  var quickStartEl = $('.js-quickstart-selector');
  var mavenWidgetEl = $('.js-quickstart-maven-widget');
  var documentationEl = $('.js-documentation-widget');

  var projectUrl = apiBaseUrl + "/project_metadata/" + projectId;
  var promise = Spring.loadProject(projectUrl);

  promise.then(function(project) {
    Spring.buildDocumentationWidget(documentationEl, project);
    Spring.buildQuickStartWidget(quickStartEl, mavenWidgetEl, project);
  });
};

Spring.buildDocumentationWidget = function(documentationEl, project) {
  new Spring.DocumentationWidgetView({
    el: documentationEl,
    model: project,
    template: $("#project-documentation-widget-template").text()
  }).render();

}
Spring.buildQuickStartWidget = function(quickStartEl, mavenWidgetEl, project) {
  var mavenWidget = new Spring.MavenSnippetView({
    el: mavenWidgetEl,
    model: project.releases[0],
    dependencyTemplate: $("#project-quickstart-maven-widget-dependency-template").text(),
    repositoryTemplate: $("#project-quickstart-maven-widget-repository-template").text()
  }).render();

  new Spring.QuickStartSelectorView({
    el: quickStartEl,
    model: project,
    template: $("#project-quickstart-selector-template").text(),
    mavenWidget: mavenWidget
  }).render();

}

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
      return "spring-icon-pre-release";
    } else if (this.current) {
      return "spring-icon-current-version";
    } else {
      return "spring-icon-supported-version";
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

Spring.DocumentationWidgetView = Backbone.View.extend({
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

Spring.MavenSnippetView = Backbone.View.extend({
  initialize: function() {
    this.dependencyTemplate = _.template(this.options.dependencyTemplate);
    this.repositoryTemplate = _.template(this.options.repositoryTemplate);
    _.bindAll(this, "render");
  },

  render: function() {
    var html = $("<pre></pre>");
    html.append(this.dependencyTemplate(this.model));
    if (this.model.repository != null) {
      html.append(this.repositoryTemplate(this.model.repository));
    }
    this.$el.html(html);
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
    this.$('.selectpicker').selectpicker();
    return this;
  },

  updateMaven: function() {
    this.mavenWidget.model = this.model.releases[this.$('.selector :selected').val()];
    this.mavenWidget.render();
  }

});
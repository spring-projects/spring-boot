describe("QuickStartWidget", function () {

  describe("rendering", function () {

    beforeEach(function () {
      var project = new Spring.Project({
        "id": "spring-data-jpa",
        "name": "Spring Data JPA",
        "repoUrl": "http://github.com/SpringSource/spring-data-jpa",
        "siteUrl": "http://projects.spring.io/spring-data-jpa",
        "projectReleases": [
          {
            "refDocUrl": "http://docs.spring.io/spring-data/jpa/docs/1.4.0.RC1/reference/html/",
            "apiDocUrl": "http://docs.spring.io/spring-data/jpa/docs/1.4.0.RC1/api/",
            "groupId": "org.springframework.data",
            "artifactId": "spring-data-jpa",
            "repository": {
              "id": "spring-milestones",
              "name": "Spring Milestones",
              "url": "http://repo.spring.io/milestone",
              "snapshotsEnabled": false
            },
            "version": "1.4.0.RC1",
            "current": false,
            "preRelease": true,
            "snapshot": false,
            "generalAvailability": false,
            "versionDisplayName": "1.4.0.RC1"
          },
          {
            "refDocUrl": "http://docs.spring.io/spring-data/jpa/docs/1.3.4.RELEASE/reference/html/",
            "apiDocUrl": "http://docs.spring.io/spring-data/jpa/docs/1.3.4.RELEASE/api/",
            "groupId": "org.springframework.data",
            "artifactId": "spring-data-jpa",
            "repository": null,
            "version": "1.3.4.RELEASE",
            "current": true,
            "preRelease": false,
            "snapshot": false,
            "generalAvailability": true,
            "versionDisplayName": "1.3.4"
          }
        ]
      });

      $('#jasmine_content').append("<div id='quick_select_widget'></div> ");
      $('#jasmine_content').append("<div id='maven_widget'></div> ");
      Spring.buildQuickStartWidget("#quick_select_widget", "#maven_widget", project);
    });

    it("lists out each release's version", function () {
      expect($('#quick_select_widget')).toContainText("1.4.0.RC1");
      expect($('#quick_select_widget')).toContainText("1.3.4");
    });

    describe("maven view", function() {
      it("shows the current release dependency by default", function() {
        expect($('#maven_widget')).toContainText("org.springframework.data");
        expect($('#maven_widget')).toContainText("spring-data-jpa");
        expect($('#maven_widget')).toContainText("1.3.4.RELEASE");
      });

      it("shows the correct dependency when users select a different release", function() {
        $('#jasmine_content select').val(0).change();

        expect($('#maven_widget')).toContainText("org.springframework.data");
        expect($('#maven_widget')).toContainText("spring-data-jpa");
        expect($('#maven_widget')).toContainText("1.4.0.RC1");
      });

      it("shows the repository information if user selects a release with a repository", function() {
          $('#jasmine_content select').val(0).change();

          expect($('#maven_widget')).toContainText("spring-milestones");
          expect($('#maven_widget')).toContainText("Spring Milestones");
          expect($('#maven_widget')).toContainText("http://repo.spring.io/milestone");
          expect($('#maven_widget')).toContainText("false");
      });

      it("doesn't show the repository if the user selects a release without a repository", function (){
          $('#jasmine_content select').val(1).change();

          expect($('#maven_widget')).not.toContainText("repository");
          expect($('#maven_widget')).not.toContainText("spring-milestones");
          expect($('#maven_widget')).not.toContainText("Spring Milestones");
          expect($('#maven_widget')).not.toContainText("http://repo.spring.io/milestone");
          expect($('#maven_widget')).not.toContainText("false");
      });
    });

    describe("gradle view", function() {
      beforeEach(function() {
        $("#quick_select_widget [data-snippet-type=gradle]").click();
      });

      it("shows the current release dependency by default", function() {
        expect($('#maven_widget')).toContainText("dependencies");
        expect($('#maven_widget')).toContainText("org.springframework.data:spring-data-jpa:1.3.4.RELEASE");
      });

      it("shows the repository if the data has one", function() {
        $('#jasmine_content select').val(0).change();

        expect($('#maven_widget')).toContainText("repositories");
        expect($('#maven_widget')).toContainText("http://repo.spring.io/milestone");
      });
    });
  });
});

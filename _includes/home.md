<div class="billboard--wrapper project-header--wrapper">
  <div class="billboard--container">
    <div class="container-fluid">
      <div  class="content--title">
      <!-- TODO: when in <p/> style of this text is wrong (font too large) -->
      <!-- TODO: why do we have to markdownify? -->
      {{ include.links | markdownify }} 
      </div>
      <div class="row-fluid">
        <div class="span8">
          <div class="project--links--container">
            <a href="{{ site.github }}" class="project-link">
              <i class="icon-github"></i>
            </a>
            <a href="{{ site.forum }}" class="project-link">
              <div class="icon icon-forum"></div>
            </a>
          </div>
          <div class="project--title">{{ page.title }}</div>
          
          <div class="project--description">
           {{ include.description | markdownify }} <!-- TODO: this div has text with wrong colour if nested in <p/> -->
          </div>
          <div class="btn btn-black uppercase project-quickstart-btn">Quick Start</div> <!-- TODO: this button not working -->
        </div>
      </div>
      
    </div>
  </div>
  <div class="billboard-bg spring-data--bg"></div> <!-- TODO: this div adds a grey background to the material above it! -->
</div> 
<div class="billboard-body--wrapper project-body--container">
  <div class="row-fluid">
    <div class="span8">
      <div class="project-body--section">
      {{ include.body | markdownify }}
      </div>
      <div class="project-body--section">

          <div class="row-fluid quickstart--container">
          <div class="quickstart--header js-item-dropdown-widget--wrapper">
            <div class="quickstart--title">
              Quick Start (TBD)
            </div>
            <div class="js-quickstart-selector"></div>

            <div class="item-slider-widget js-item-slider--wrapper">
              <div class="item-slider--container">
                <div class="item--slider js-item--slider"></div>
                <div class="item js-active js-item">
                  Maven
                </div>
                <div class="item js-item">
                  Gradle
                </div>
                <div class="item js-item">
                  Zip
                </div>
              </div>
            </div>
          </div>
          <div class="quickstart--body">
            {{ include.quickstart | markdownify }}
            <div class="highlight"><button class="copy-button snippet" id="copy-button-2" data-clipboard-target="code-block-2"></button>
                <div class="js-quickstart-maven-widget"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="span4">
        {% include documentation.html %}

      <div class="right-pane-widget--container no-top-border">
        <div class="project-sub-link--wrapper">
        {{ include.projects | markdownify }}
        </div>
      </div>
      <div class="right-pane-widget--container no-top-border project-additional-resource--wrapper">
        {{ include.additional | markdownify }}
      </div>
    </div>
  </div>
</div>

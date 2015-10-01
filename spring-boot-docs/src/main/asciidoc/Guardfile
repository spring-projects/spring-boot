require 'asciidoctor'
require 'erb'

guard 'shell' do
	watch(/.*\.adoc$/) {|m|
		Asciidoctor.render_file('index.adoc', \
			:in_place => true, \
			:safe => Asciidoctor::SafeMode::UNSAFE, \
			:attributes=> { \
				'source-highlighter' => 'prettify', \
				'icons' => 'font', \
				'linkcss'=> 'true', \
				'copycss' => 'true', \
				'doctype' => 'book'})
	}
end

guard 'livereload' do
	watch(%r{^.+\.(css|js|html)$})
end

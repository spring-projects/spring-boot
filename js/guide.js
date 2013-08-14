ZeroClipboard.setDefaults( { moviePath: '/js/ZeroClipboard.swf' } );

$(document).ready(function() {
    if (ZeroClipboard.detectFlashSupport()) {
        createCodeCopyButtons();
        createCopyButton($('button.copy-button.github'));
    }

    $('.github-actions button').click(function() {
        $('.github-actions button').removeClass('active');
        $(this).addClass('active');

        $('.clone-url').hide();
        $('.clone-url.' + $(this).data('protocol')).show();
    });

    if (typeof(sts_import) === 'function') {
        $(".gs-guide-import").show().click(function (e) {
            var linkElement = e.target;
            var url = linkElement.href;
            sts_import("guide", url);
            e.preventDefault();
        });
    }

});

function createCodeCopyButtons() {
    $('article .highlight pre').each(function(index) {
            var codeBlockId = "code-block-"+ index;
            $(this).attr('id', codeBlockId);
            var button = $('<button class="copy-button snippet" id="copy-button-"' + index + ' data-clipboard-target="' + codeBlockId + '"></button>');
            $(this).before(button);
            createCopyButton(button);
        }
    );
}

function createCopyButton($el){
    var zero = new ZeroClipboard($el);
    $(zero.htmlBridge).tooltip({title: "copy to clipboard", placement: 'bottom'});
}
/*!
 * bootstrap-select v1.1.1
 * http://silviomoreto.github.io/bootstrap-select/
 *
 * Copyright 2013 bootstrap-select
 * Licensed under the MIT license
 */

!function($) {

    "use strict";

    var Selectpicker = function(element, options, e) {
        if (e) {
            e.stopPropagation();
            e.preventDefault();
        }
        this.$element = $(element);
        this.$newElement = null;
        this.$button = null;
        this.$menu = null;

        //Merge defaults, options and data-attributes to make our options
        this.options = $.extend({}, $.fn.selectpicker.defaults, this.$element.data(), typeof options == 'object' && options);

        //If we have no title yet, check the attribute 'title' (this is missed by jq as its not a data-attribute
        if (this.options.title == null) {
            this.options.title = this.$element.attr('title');
        }

        //Expose public methods
        this.val = Selectpicker.prototype.val;
        this.render = Selectpicker.prototype.render;
        this.refresh = Selectpicker.prototype.refresh;
        this.setStyle = Selectpicker.prototype.setStyle;
        this.selectAll = Selectpicker.prototype.selectAll;
        this.deselectAll = Selectpicker.prototype.deselectAll;
        this.init();
    };

    Selectpicker.prototype = {

        constructor: Selectpicker,

        init: function(e) {
            this.$element.hide();
            this.multiple = this.$element.prop('multiple');
            var id = this.$element.attr('id');
            this.$newElement = this.createView();
            this.$element.after(this.$newElement);
            this.$menu = this.$newElement.find('> .dropdown-menu');
            this.$button = this.$newElement.find('> button');

            if (id !== undefined) {
                var _this = this;
                this.$button.attr('data-id', id);
                $('label[for="' + id + '"]').click(function() {
                    _this.$button.focus();
                });
            }

            //If we are multiple, then add the show-tick class by default
            if (this.multiple) {
                 this.$newElement.addClass('show-tick');
            }

            this.checkDisabled();
            this.checkTabIndex();
            this.clickListener();
            this.render();
            this.liHeight();
            this.setWidth();
            this.setStyle();
            if (this.options.container) {
                this.selectPosition();
            }
            this.$menu.data('this', this);
            this.$newElement.data('this', this);
        },

        createDropdown: function() {
            var drop =
                "<div class='btn-group bootstrap-select'>" +
                    "<button type='button' class='dropdown-toggle' data-toggle='dropdown'>" +
                        "<div class='item-dropdown--icon icon-reorder pull-right'></div>" +
                        "<div class='filter-option pull-left'></div>&nbsp;" +
                    "</button>" +
                    "<div class='dropdown-menu open'>" +
                        "<ul class='dropdown-menu inner' role='menu'>" +
                        "</ul>" +
                    "</div>" +
                "</div>";

            return $(drop);
        },

        createView: function() {
            var $drop = this.createDropdown();
            var $li = this.createLi();
            $drop.find('ul').append($li);
            return $drop;
        },

        reloadLi: function() {
            //Remove all children.
            this.destroyLi();
            //Re build
            var $li = this.createLi();
            this.$newElement.find('ul').append( $li );
        },

        destroyLi: function() {
            this.$newElement.find('li').remove();
        },

        createLi: function() {
            var _this = this,
                _liA = [],
                _liHtml = '';

            this.$element.find('option').each(function(index) {
                var $this = $(this);

                //Get the class and text for the option
                var optionClass = $this.attr("class") || '';
                var inline = $this.attr("style") || '';
                var text =  $this.data('content') ? $this.data('content') : $this.html();
                var subtext = $this.data('subtext') !== undefined ? '<small class="muted">' + $this.data('subtext') + '</small>' : '';
                var icon = $this.data('icon') !== undefined ? '<i class="glyphicon '+$this.data('icon')+'"></i> ' : '';
                if (icon !== '' && ($this.is(':disabled') || $this.parent().is(':disabled'))) {
                    icon = '<span>'+icon+'</span>';
                }

                if (!$this.data('content')) {
                    //Prepend any icon and append any subtext to the main text.
                    text = icon + '<span class="text">' + text + subtext + '</span>';
                }

                if (_this.options.hideDisabled && ($this.is(':disabled') || $this.parent().is(':disabled'))) {
                    _liA.push('<a style="min-height: 0; padding: 0"></a>');
                } else if ($this.parent().is('optgroup') && $this.data('divider') != true) {
                    if ($this.index() == 0) {
                        //Get the opt group label
                        var label = $this.parent().attr('label');
                        var labelSubtext = $this.parent().data('subtext') !== undefined ? '<small class="muted">'+$this.parent().data('subtext')+'</small>' : '';
                        var labelIcon = $this.parent().data('icon') ? '<i class="'+$this.parent().data('icon')+'"></i> ' : '';
                        label = labelIcon + '<span class="text">' + label + labelSubtext + '</span>';

                        if ($this[0].index != 0) {
                            _liA.push(
                                '<div class="div-contain"><div class="divider"></div></div>'+
                                '<dt>'+label+'</dt>'+
                                _this.createA(text, "opt " + optionClass, inline )
                                );
                        } else {
                            _liA.push(
                                '<dt>'+label+'</dt>'+
                                _this.createA(text, "opt " + optionClass, inline ));
                        }
                    } else {
                         _liA.push( _this.createA(text, "opt " + optionClass, inline ) );
                    }
                } else if ($this.data('divider') == true) {
                    _liA.push('<div class="div-contain"><div class="divider"></div></div>');
                } else if ($(this).data('hidden') == true) {
                    _liA.push('');
                } else {
                    _liA.push( _this.createA(text, optionClass, inline ) );
                }
            });

            $.each(_liA, function(i, item) {
                _liHtml += "<li rel=" + i + ">" + item + "</li>";
            });

            //If we are not multiple, and we dont have a selected item, and we dont have a title, select the first element so something is set in the button
            if (!this.multiple && this.$element.find('option:selected').length==0 && !_this.options.title) {
                this.$element.find('option').eq(0).prop('selected', true).attr('selected', 'selected');
            }

            return $(_liHtml);
        },

        createA: function(text, classes, inline) {
            return '<a tabindex="0" class="'+classes+'" style="'+inline+'">' +
                 text +
                 '<i class="glyphicon glyphicon-ok icon-ok check-mark"></i>' +
                 '</a>';
        },

        render: function() {
            var _this = this;

            //Update the LI to match the SELECT
            this.$element.find('option').each(function(index) {
               _this.setDisabled(index, $(this).is(':disabled') || $(this).parent().is(':disabled') );
               _this.setSelected(index, $(this).is(':selected') );
            });

            var selectedItems = this.$element.find('option:selected').map(function(index,value) {
                var $this = $(this);
                var icon = $this.data('icon') && _this.options.showIcon ? '<i class="glyphicon ' + $this.data('icon') + '"></i> ' : '';
                var subtext;
                if (_this.options.showSubtext && $this.attr('data-subtext') && !_this.multiple) {
                    subtext = ' <small class="muted">'+$this.data('subtext') +'</small>';
                } else {
                    subtext = '';
                }
                if ($this.data('content') && _this.options.showContent) {
                    return $this.data('content');
                } else if ($this.attr('title') != undefined) {
                    return $this.attr('title');
                } else {
                    return icon + $this.html() + subtext;
                }
            }).toArray();

            //Fixes issue in IE10 occurring when no default option is selected and at least one option is disabled
            //Convert all the values into a comma delimited string
            var title = !this.multiple ? selectedItems[0] : selectedItems.join(", ");

            //If this is multi select, and the selectText type is count, the show 1 of 2 selected etc..
            if (_this.multiple && _this.options.selectedTextFormat.indexOf('count') > -1) {
                var max = _this.options.selectedTextFormat.split(">");
                var notDisabled = this.options.hideDisabled ? ':not([disabled])' : '';
                if ( (max.length>1 && selectedItems.length > max[1]) || (max.length==1 && selectedItems.length>=2)) {
                    title = _this.options.countSelectedText.replace('{0}', selectedItems.length).replace('{1}', this.$element.find('option:not([data-divider="true"]):not([data-hidden="true"])'+notDisabled).length);
                }
             }

            //If we dont have a title, then use the default, or if nothing is set at all, use the not selected text
            if (!title) {
                title = _this.options.title != undefined ? _this.options.title : _this.options.noneSelectedText;
            }

            _this.$newElement.find('.filter-option').html(title);
        },

        setStyle: function(style, status) {
            if (this.$element.attr('class')) {
                this.$newElement.addClass(this.$element.attr('class').replace(/selectpicker|mobile-device/gi, ''));
            }

            var buttonClass = style ? style : this.options.style;

            if (status == 'add') {
                this.$button.addClass(buttonClass);
            } else {
                this.$button.removeClass(this.options.style);
                this.$button.addClass(buttonClass);
            }
        },

        liHeight: function() {
            var selectClone = this.$newElement.clone();
            selectClone.appendTo('body');
            var liHeight = selectClone.addClass('open').find('.dropdown-menu li > a').outerHeight();
            selectClone.remove();
            this.$newElement.data('liHeight', liHeight);
        },

        setSize: function() {
            var _this = this,
                menu = this.$menu,
                menuInner = menu.find('.inner'),
                menuA = menuInner.find('li > a'),
                selectHeight = this.$newElement.outerHeight(),
                liHeight = this.$newElement.data('liHeight'),
                divHeight = menu.find('li .divider').outerHeight(true),
                menuPadding = parseInt(menu.css('padding-top')) +
                              parseInt(menu.css('padding-bottom')) +
                              parseInt(menu.css('border-top-width')) +
                              parseInt(menu.css('border-bottom-width')),
                notDisabled = this.options.hideDisabled ? ':not(.disabled)' : '',
                $window = $(window),
                menuExtras = menuPadding + parseInt(menu.css('margin-top')) + parseInt(menu.css('margin-bottom')) + 2,
                menuHeight,
                selectOffsetTop,
                selectOffsetBot,
                posVert = function() {
                    selectOffsetTop = _this.$newElement.offset().top - $window.scrollTop();
                    selectOffsetBot = $window.height() - selectOffsetTop - selectHeight;
                };
                posVert();
                
            if (this.options.size == 'auto') {
                var getSize = function() {
                    var minHeight;
                    posVert();
                    menuHeight = selectOffsetBot - menuExtras;
                    _this.$newElement.toggleClass('dropup', (selectOffsetTop > selectOffsetBot) && (menuHeight - menuExtras) < menu.height() && _this.options.dropupAuto);
                    if (_this.$newElement.hasClass('dropup')) {
                        menuHeight = selectOffsetTop - menuExtras;
                    }
                    if ((menu.find('li').length + menu.find('dt').length) > 3) {
                        minHeight = liHeight*3 + menuExtras - 2;
                    } else {
                        minHeight = 0;
                    }
                    menu.css({'max-height' : menuHeight + 'px', 'overflow' : 'hidden', 'min-height' : minHeight + 'px'});
                    menuInner.css({'max-height' : (menuHeight - menuPadding) + 'px', 'overflow-y' : 'auto', 'min-height' : (minHeight - menuPadding) + 'px'});
                }
                getSize();
                $(window).resize(getSize);
                $(window).scroll(getSize);
            } else if (this.options.size && this.options.size != 'auto' && menu.find('li'+notDisabled).length > this.options.size) {
                var optIndex = menu.find("li"+notDisabled+" > *").filter(':not(.div-contain)').slice(0,this.options.size).last().parent().index();
                var divLength = menu.find("li").slice(0,optIndex + 1).find('.div-contain').length;
                menuHeight = liHeight*this.options.size + divLength*divHeight + menuPadding;
                this.$newElement.toggleClass('dropup', (selectOffsetTop > selectOffsetBot) && menuHeight < menu.height() && this.options.dropupAuto);
                menu.css({'max-height' : menuHeight + 'px', 'overflow' : 'hidden'});
                menuInner.css({'max-height' : (menuHeight - menuPadding) + 'px', 'overflow-y' : 'auto'});
            }
        },

        setWidth: function() {
            //Set width of select
            if (this.options.width == 'auto') {
                this.$menu.css('min-width','0');

                // Get correct width if element hidden
                var selectClone = this.$newElement.clone().appendTo('body');
                var ulWidth = selectClone.find('> .dropdown-menu').css('width');
                selectClone.remove();

                this.$newElement.css('width',ulWidth);
            } else if (this.options.width) {
                this.$newElement.css('width',this.options.width);
            }
        },

        selectPosition: function() {
            var _this = this,
                drop = "<div />",
                $drop = $(drop),
                pos,
                actualHeight,
                getPlacement = function($element) {
                    $drop.addClass($element.attr('class')).toggleClass('dropup', $element.hasClass('dropup'));
                    pos = $element.offset();
                    actualHeight = $element.hasClass('dropup') ? 0 : $element[0].offsetHeight;
                    $drop.css({'top' : pos.top + actualHeight, 'left' : pos.left, 'width' : $element[0].offsetWidth, 'position' : 'absolute'});
                };
            this.$newElement.on('click', function(e) {
                getPlacement($(this));
                $drop.appendTo(_this.options.container);
                $drop.toggleClass('open', !$(this).hasClass('open'));
                $drop.append(_this.$menu);
            });
            $(window).resize(function() {
                getPlacement(_this.$newElement);
            });
            $(window).on('scroll', function(e) {
                getPlacement(_this.$newElement);
            });
            $('html').on('click', function(e) {
                if ($(e.target).closest(_this.$newElement).length < 1) {
                    $drop.removeClass('open');
                }
            });
        },

        mobile: function() {
            this.$element.addClass('mobile-device').appendTo(this.$newElement);
            if (this.options.container) this.$menu.hide();
        },

        refresh: function() {
            this.reloadLi();
            this.render();
            this.setWidth();
            this.setStyle();
            this.checkDisabled();
        },

        setSelected: function(index, selected) {
            this.$menu.find('li').eq(index).toggleClass('selected', selected);
        },

        setDisabled: function(index, disabled) {
            if (disabled) {
                this.$menu.find('li').eq(index).addClass('disabled').find('a').attr('href','#').attr('tabindex',-1);
            } else {
                this.$menu.find('li').eq(index).removeClass('disabled').find('a').removeAttr('href').attr('tabindex',0);
            }
        },

        isDisabled: function() {
            return this.$element.is(':disabled');
        },

        checkDisabled: function() {
            var _this = this;
            if (this.isDisabled()) {
                this.$button.addClass('disabled');
                this.$button.attr('tabindex','-1');
            } else if (this.$button.hasClass('disabled')) {
                this.$button.removeClass('disabled');
                this.$button.removeAttr('tabindex');
            }
            this.$button.click(function() {
                return !_this.isDisabled();
            });
        },

        checkTabIndex: function() {
            if (this.$element.is('[tabindex]')) {
                var tabindex = this.$element.attr("tabindex");
                this.$button.attr('tabindex', tabindex);
            }
        },

        clickListener: function() {
            var _this = this;

            $('body').on('touchstart.dropdown', '.dropdown-menu', function(e) {
                e.stopPropagation();
            });

            this.$newElement.on('click', function() {
                _this.setSize();
            });

            this.$menu.on('click', 'li a', function(e) {
                var clickedIndex = $(this).parent().index(),
                    $this = $(this).parent(),
                    prevValue = _this.$element.val();

                //Dont close on multi choice menu
                if (_this.multiple) {
                    e.stopPropagation();
                }

                e.preventDefault();

                //Dont run if we have been disabled
                if (!_this.isDisabled() && !$(this).parent().hasClass('disabled')) {
                    var $options = _this.$element.find('option');
                    var $option = $options.eq(clickedIndex);

                    //Deselect all others if not multi select box
                    if (!_this.multiple) {
                        $options.prop('selected', false);
                        $option.prop('selected', true);
                    }
                    //Else toggle the one we have chosen if we are multi select.
                    else {
                        var state = $option.prop('selected');

                        $option.prop('selected', !state);
                    }

                    _this.$button.focus();

                    // Trigger select 'change'
                    if (prevValue != _this.$element.val()) {
                        _this.$element.change();
                    }
                }
            });

            this.$menu.on('click', 'li.disabled a, li dt, li .div-contain', function(e) {
                e.preventDefault();
                e.stopPropagation();
                _this.$button.focus();
            });

            this.$element.change(function() {
                _this.render()
            });
        },

        val: function(value) {

            if (value != undefined) {
                this.$element.val( value );

                this.$element.change();
                return this.$element;
            } else {
                return this.$element.val();
            }
        },

        selectAll: function() {
            this.$element.find('option').prop('selected', true).attr('selected', 'selected');
            this.render();
        },

        deselectAll: function() {
            this.$element.find('option').prop('selected', false).removeAttr('selected');
            this.render();
        },

        keydown: function(e) {
            var $this,
                $items,
                $parent,
                index,
                next,
                first,
                last,
                prev,
                nextPrev,
                that;

            $this = $(this);

            $parent = $this.parent();

            that = $parent.data('this');

            if (that.options.container) $parent = that.$menu;

            $items = $('[role=menu] li:not(.divider):visible a', $parent);

            if (!$items.length) return;

            if (/(38|40)/.test(e.keyCode)) {

                index = $items.index($items.filter(':focus'));
                first = $items.parent(':not(.disabled)').first().index();
                last = $items.parent(':not(.disabled)').last().index();
                next = $items.eq(index).parent().nextAll(':not(.disabled)').eq(0).index();
                prev = $items.eq(index).parent().prevAll(':not(.disabled)').eq(0).index();
                nextPrev = $items.eq(next).parent().prevAll(':not(.disabled)').eq(0).index();

                if (e.keyCode == 38) {
                    if (index != nextPrev && index > prev) index = prev;
                    if (index < first) index = first;
                }

                if (e.keyCode == 40) {
                    if (index != nextPrev && index < next) index = next;
                    if (index > last) index = last;
                    if (index == -1) index = 0;
                }

                $items.eq(index).focus();
            } else {
                var keyCodeMap = {
                    48:"0", 49:"1", 50:"2", 51:"3", 52:"4", 53:"5", 54:"6", 55:"7", 56:"8", 57:"9", 59:";",
                    65:"a", 66:"b", 67:"c", 68:"d", 69:"e", 70:"f", 71:"g", 72:"h", 73:"i", 74:"j", 75:"k", 76:"l",
                    77:"m", 78:"n", 79:"o", 80:"p", 81:"q", 82:"r", 83:"s", 84:"t", 85:"u", 86:"v", 87:"w", 88:"x", 89:"y", 90:"z",
                    96:"0", 97:"1", 98:"2", 99:"3", 100:"4", 101:"5", 102:"6", 103:"7", 104:"8", 105:"9"
                }

                var keyIndex = [];

                $items.each(function() {
                    if ($(this).parent().is(':not(.disabled)')) {
                        if ($.trim($(this).text().toLowerCase()).substring(0,1) == keyCodeMap[e.keyCode]) {
                            keyIndex.push($(this).parent().index());
                        }
                    }
                });

                var count = $(document).data('keycount');
                count++;
                $(document).data('keycount',count);

                var prevKey = $.trim($(':focus').text().toLowerCase()).substring(0,1);

                if (prevKey != keyCodeMap[e.keyCode]) {
                    count = 1;
                    $(document).data('keycount',count);
                } else if (count >= keyIndex.length) {
                    $(document).data('keycount',0);
                }

                $items.eq(keyIndex[count - 1]).focus();
            }

            // select focused option if "Enter" or "Spacebar" are pressed
            if (/(13|32)/.test(e.keyCode)) {
                $(':focus').click();
                if (!that.multiple) {
                    $parent.parent().toggleClass('open', !(e.keyCode == 32));
                } else {
                    e.preventDefault();
                };
                $(document).data('keycount',0);
            }
        },

        hide: function() {
            this.$newElement.hide();
        },

        show: function() {
            this.$newElement.show();
        },

        destroy: function() {
            this.$newElement.remove();
            this.$element.remove();
        }
    };

    $.fn.selectpicker = function(option, event) {
       //get the args of the outer function..
       var args = arguments;
       var value;
       var chain = this.each(function() {
            if ($(this).is('select')) {
                var $this = $(this),
                    data = $this.data('selectpicker'),
                    options = typeof option == 'object' && option;

                if (!data) {
                    $this.data('selectpicker', (data = new Selectpicker(this, options, event)));
                } else if (options) {
                    for(var i in options) {
                       data.options[i] = options[i];
                    }
                }

                if (typeof option == 'string') {
                    //Copy the value of option, as once we shift the arguments
                    //it also shifts the value of option.
                    var property = option;
                    if (data[property] instanceof Function) {
                        [].shift.apply(args);
                        value = data[property].apply(data, args);
                    } else {
                        value = data.options[property];
                    }
                }
            }
        });

        if (value != undefined) {
            return value;
        } else {
            return chain;
        }
    };

    $.fn.selectpicker.defaults = {
        style: null,
        size: 'auto',
        title: null,
        selectedTextFormat : 'values',
        noneSelectedText : 'Nothing selected',
        countSelectedText: '{0} of {1} selected',
        width: null,
        container: false,
        hideDisabled: false,
        showSubtext: false,
        showIcon: true,
        showContent: true,
        dropupAuto: true
    }

    $(document)
        .data('keycount', 0)
        .on('keydown', '[data-toggle=dropdown], [role=menu]' , Selectpicker.prototype.keydown)

}(window.jQuery);
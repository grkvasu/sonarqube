/*global alert:false, Backbone:false, Spinner:false*/

(function ($) {

  /*
   * SelectList Collection
   */

  var SelectListCollection = Backbone.Collection.extend({

    parse: function(r) {
      this.more = r.more;
      return r.results;
    },

    fetch: function(options) {
      var data = $.extend({
            page: 1,
            pageSize: 100
          }, options.data || {}),
          settings = $.extend({}, options, { data: data });

      this.settings = {
        url: settings.url,
        data: data
      };

      Backbone.Collection.prototype.fetch.call(this, settings);
    },

    fetchNextPage: function(options) {
      if (this.more) {
        var nextPage = this.settings.data.page + 1,
            settings = $.extend(this.settings, options);

        settings.data.page = nextPage;

        this.fetch(settings);
      }
    }

  });



  /*
   * SelectList Item View
   */

  var SelectListItemView = Backbone.View.extend({
    tagName: 'li',

    checkboxTemplate: '<a class="select-list-list-checkbox"></a>',

    events: {
      'click .select-list-list-checkbox': 'toggle'
    },

    initialize: function(options) {
      this.listenTo(this.model, 'change', this.render);
      this.settings = options.settings;
    },

    render: function() {
      this.$el.empty()
          .append(this.checkboxTemplate)
          .append(this.settings.format(this.model.toJSON()));
      this.$el.toggleClass('selected', this.model.get('selected'));
      this.$('.select-list-list-checkbox').attr('title',
          this.model.get('selected') ?
              this.settings.tooltips.deselect :
              this.settings.tooltips.select);
    },

    toggle: function() {
      var selected = this.model.get('selected'),
          that = this;

      this.$('.select-list-list-checkbox').addClass('with-spinner');
      new Spinner(this.settings.spinnerSmall)
          .spin(this.$('.select-list-list-checkbox')[0]);

      var url = selected ? this.settings.deselectUrl : this.settings.selectUrl;
      $.ajax({
          url: url,
          type: 'POST',
          data: { user: this.model.id }
      })
          .done(function() {
            that.model.set('selected', !selected);
          })
          .fail(function() {
            alert(that.settings.errorMessage);
          });
    }
  });



  /*
   * SelectList View
   */

  var SelectListView = Backbone.View.extend({
    template: function(l) {
      return '<div class="select-list-container">' +
          '<div class="select-list-control">' +
            '<div class="select-list-check-control">' +
              '<a class="select-list-control-button" name="selected">' + l.selected + '</a>' +
              '<a class="select-list-control-button" name="deselected">' + l.deselected + '</a>' +
              '<a class="select-list-control-button" name="all">' + l.all + '</a>' +
            '</div>' +
            '<div class="select-list-search-control">' +
              '<input type="text" placeholder="Search">' +
              '<a class="select-list-search-control-clear">&times;</a>' +
            '</div>' +
          '</div>' +
          '<div class="select-list-list-container">' +
            '<ul class="select-list-list"></ul>' +
          '</div>' +
        '</div>';
    },

    events: {
      'click .select-list-control-button[name=selected]': 'showSelected',
      'click .select-list-control-button[name=deselected]': 'showDeselected',
      'click .select-list-control-button[name=all]': 'showAll',

      'click .select-list-search-control-clear': 'clearSearch'
    },

    initialize: function(options) {
      this.listenTo(this.collection, 'add', this.renderListItem);
      this.listenTo(this.collection, 'reset', this.renderList);
      this.settings = options.settings;
    },

    render: function() {
      var that = this,
          keyup = function() { that.search(); };

      this.$el.html(this.template(this.settings.labels))
          .width(this.settings.width);

      this.$listContainer = this.$('.select-list-list-container')
          .height(this.settings.height)
          .css('overflow', 'scroll')
          .on('scroll', function() { that.scroll(); });

      this.$list = this.$('.select-list-list');

      this.$('.select-list-search-control input')
          .focus()
          .on('keyup', $.debounce(250, keyup));

      this.listItemViews = [];
    },

    renderList: function() {
      this.listItemViews.forEach(function(view) { view.remove(); });
      this.collection.each(this.renderListItem, this);
      this.$listContainer.scrollTop(0);
    },

    renderListItem: function(item) {
      var itemView = new SelectListItemView({
        model: item,
        settings: this.settings
      });
      this.listItemViews.push(itemView);
      this.$list.append(itemView.el);
      itemView.render();
    },

    filterBySelection: function(filter) {
      var that = this;
      filter = this.currentFilter = filter || this.currentFilter;

      if (filter != null) {
        this.$('.select-list-check-control').toggleClass('disabled', false);
        this.$('.select-list-search-control').toggleClass('disabled', true);
        this.$('.select-list-search-control input').val('');

        this.$('.select-list-control-button').removeClass('active')
            .filter('[name=' + filter + ']').addClass('active');

        this.showFetchSpinner();

        this.collection.fetch({
          url: this.settings.searchUrl,
          reset: true,
          data: { selected: filter },
          success: function() {
            that.hideFetchSpinner();
          },
          error: function() {
            alert(that.settings.errorMessage);
          }
        });
      }
    },

    showSelected: function() {
      this.filterBySelection('selected');
    },

    showDeselected: function() {
      this.filterBySelection('deselected');
    },

    showAll: function() {
      this.filterBySelection('all');
    },

    search: function() {
      var query = this.$('.select-list-search-control input').val(),
          hasQuery = query.length > 0,
          that = this;

      this.$('.select-list-check-control').toggleClass('disabled', hasQuery);
      this.$('.select-list-search-control').toggleClass('disabled', !hasQuery);

      if (hasQuery) {
        this.showFetchSpinner();

        this.collection.fetch({
          url: this.settings.searchUrl,
          reset: true,
          data: { query: query },
          success: function() {
            that.hideFetchSpinner();
          },
          error: function() {
            alert(that.settings.errorMessage);
          }
        });
      } else {
        this.filterBySelection();
      }
    },

    searchByQuery: function(query) {
      this.$('.select-list-search-control input').val(query);
      this.search();
    },

    clearSearch: function() {
      this.filterBySelection();
    },

    showFetchSpinner: function() {
      var options = $.extend(this.settings.spinnerBig, {
        className: 'select-list-spinner'
      });
      new Spinner(options).spin(this.$el[0]);
    },

    hideFetchSpinner: function() {
      this.$('.select-list-spinner').remove();
    },

    scroll: function() {
      var scrollBottom = this.$listContainer.scrollTop() >=
          this.$list[0].scrollHeight - this.$listContainer.outerHeight(),
          that = this;

      if (scrollBottom && this.collection.more) {
        $.throttle(250, function() {
            that.showFetchSpinner();

            that.collection.fetchNextPage({
              success: function() { that.hideFetchSpinner(); }
            });
        })();
      }
    }

  });



  /*
   * SelectList Entry Point
   */

  window.SelectList = function(options) {
    this.settings = $.extend(window.SelectList.defaults, options);

    this.collection = new SelectListCollection();

    this.view = new SelectListView({
      el: this.settings.el,
      collection: this.collection,
      settings: this.settings
    });

    this.view.render();
    this.filter('selected');
    return this;
  };



  /*
   * SelectList API Methods
   */

  window.SelectList.prototype.filter = function(filter) {
    this.view.filterBySelection(filter);
    return this;
  };

  window.SelectList.prototype.search = function(query) {
    this.view.searchByQuery(query);
    return this;
  };



  /*
   * SelectList Defaults
   */

  window.SelectList.defaults = {
    width: '50%',
    height: 400,

    format: function (item) { return item.value; },

    labels: {
      selected: 'Selected',
      deselected: 'Deselected',
      all: 'All'
    },

    tooltips: {
      select: 'Click this to select item',
      deselect: 'Click this to deselect item'
    },

    errorMessage: 'Something gone wrong, try to reload the page and try again.',

    spinnerSmall: {
      lines: 9, // The number of lines to draw
      length: 0, // The length of each line
      width: 2, // The line thickness
      radius: 4, // The radius of the inner circle
      corners: 1, // Corner roundness (0..1)
      rotate: 0, // The rotation offset
      direction: 1, // 1: clockwise, -1: counterclockwise
      color: '#4b9fd5', // #rgb or #rrggbb or array of colors
      speed: 2, // Rounds per second
      trail: 60, // Afterglow percentage
      shadow: false, // Whether to render a shadow
      hwaccel: false, // Whether to use hardware acceleration
      className: 'spinner', // The CSS class to assign to the spinner
      zIndex: 2e9, // The z-index (defaults to 2000000000)
      top: 'auto', // Top position relative to parent in px
      left: 'auto' // Left position relative to parent in px
    },

    spinnerBig: {
      lines: 9, // The number of lines to draw
      length: 0, // The length of each line
      width: 6, // The line thickness
      radius: 16, // The radius of the inner circle
      corners: 1, // Corner roundness (0..1)
      rotate: 0, // The rotation offset
      direction: 1, // 1: clockwise, -1: counterclockwise
      color: '#4b9fd5', // #rgb or #rrggbb or array of colors
      speed: 2, // Rounds per second
      trail: 60, // Afterglow percentage
      shadow: false, // Whether to render a shadow
      hwaccel: false, // Whether to use hardware acceleration
      className: 'spinner', // The CSS class to assign to the spinner
      zIndex: 2e9, // The z-index (defaults to 2000000000)
      top: 'auto', // Top position relative to parent in px
      left: 'auto' // Left position relative to parent in px
    }
  };

})(jQuery);

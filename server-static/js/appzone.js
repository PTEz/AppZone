/*global Backbone,_ */
(function() {
var SERVER = 'http://appzone-api.pes.ch/';
// var SERVER = 'http://localhost:8080/';
var AppItem = Backbone.Model.extend({
  url: function() { return SERVER + 'app/' + this.id; },
  clear: function() {
    this.destroy();
  },
  parse: function(data) {
    function doParse(name) {
      var items = [];
      var keys = _.keys(data[name]);
      for (var i = 0; i < keys.length; i++) {
        var item = _.clone(data[name][keys[i]]);
        item.appId = data.id;
        item.id = keys[i];
        item.platform = name;
        items.push(item);
      }
      return _.sortBy(items, function(item) { return item.id; } );
    }

    data.ios = doParse('ios');
    data.android = doParse('android');
    return data;
  }
});

var AppItemList = Backbone.Collection.extend({
  model: AppItem,
  url: SERVER + 'apps',
  parse: function(items) {
    return _.filter(items, function(item) { return item.id.indexOf('_') !== 0; });
  }
});

var AppItemView = Backbone.View.extend({
  tagName:  'li',
  template: _.template($('#app-template').html()),
  events: {
    'click .delete.all': 'deleteApp'
  },
  render: function() {
    var app = this.model;
    this.$el.html(this.template(app.toJSON()));
    for (var i = 0; i < app.attributes.android.length; i++) {
      var view = new ReleaseItemView({model: app.attributes.android[i]});
      this.$('.android').append(view.render().el);
    }
    for (var j = 0; j < app.attributes.ios.length; j++) {
      var view2 = new ReleaseItemView({model: app.attributes.ios[j]});
      this.$('.ios').append(view2.render().el);
    }
    if (app.attributes.android.length > 0 && app.attributes.ios.length === 0) {
      this.$('.android').addClass('fill');
      this.$('.ios').hide();
    }
    if (app.attributes.ios.length > 0 && app.attributes.android.length === 0) {
      this.$('.ios').addClass('fill');
      this.$('.android').hide();
    }
    return this;
  },
  deleteApp: function() {
    if(prompt('Type "DELETE" to really delete') === 'DELETE') {
      var url = SERVER + 'app/' + this.model.id;
      $.ajax({type: 'DELETE', url: url});
      this.$el.remove();
    }
    return false;
  }
});

var ReleaseItemView = Backbone.View.extend({
  tagName: 'li',
  template: _.template($('#release-template').html()),
  events: {
    'click .delete': 'deleteRelease'
  },
  render: function() {
    this.$el.html(this.template(this.model));
    this.$('a.download').attr('href', SERVER + 'app/' + this.model.appId + '/' +this.model.platform + '/' + this.model.id);
    return this;
  },
  deleteRelease: function() {
    if(prompt('Type "DELETE" to really delete') === 'DELETE') {
      var url = SERVER + 'app/' + this.model.appId + '/' + this.model.platform + '/' + this.model.id;
      $.ajax({type: 'DELETE', url: url});
      this.$el.remove();
    }
    return false;
  }
});

var Feedback = Backbone.Model.extend({
});

var FeedbackList = Backbone.Collection.extend({
  model: Feedback,
  url: function() { return SERVER + 'app/' + this.appId + '/feedback' ; }
});

var FeedbackView = Backbone.View.extend({
  tagName:  'li',
  template: _.template($('#feedback-template').html()),
  render: function() {
    this.$el.html(this.template(this.model.toJSON()));
    return this;
  }
});


//////
// App
//////
var AppsView = Backbone.View.extend({
  el: $('#appzoneapp'),
  apps: new AppItemList(),
  initialize: function() {
    var that = this;
    this.apps.fetch({
      success: function() { that.render.call(that); }
    });
  },
  render: function() {
    this.apps.each(function(app) {
      var view = new AppItemView({model: app});
      this.$('#app-list').append(view.render().el);
    });
  },
  destroy: function() {
    $('#app-list').children().remove();
  }
});

var AppView = Backbone.View.extend({
  el: $('#appzoneapp'),
  apps: new AppItemList(),
  feedbacks: new FeedbackList(),
  events: {
    'click input.submit':  'sendFeedback',
    'keyup textarea[name=feedback]': 'validateForm'
  },
  initialize: function() {
    var that = this;
    this.apps.remove(this.apps.models.slice(0));
    this.apps.add([{id: this.id}]);
    this.apps.get(this.id).fetch({
      success: function() { that.render.call(that); }
    });
    this.feedbacks.appId = this.id;
    this.feedbacks.fetch({
      success: function() { that.render.call(that); }
    });
    $('#app-feedback').html(_.template($('#feedback-form-template').html()));
    this.validateForm();
  },
  render: function() {
    $('#app-list').children().remove();
    this.apps.each(function(app) {
      var view = new AppItemView({model: app});
      this.$('#app-list').append(view.render().el);
    });
    $('#feedbacks').children().remove();
    this.feedbacks.each(function(app) {
      var view = new FeedbackView({model: app});
      this.$('#feedbacks').append(view.render().el);
    });
  },
  destroy: function() {
    $('#app-list').children().remove();
    $('#app-feedback').children().remove();
    $('#feedbacks').children().remove();
  },
  validateForm: function() {
    var enable = this.$('textarea[name=feedback]').val().length > 0;
    if (enable) {
      $('input[type=submit]').removeAttr('disabled');
    } else {
      $('input[type=submit]').attr('disabled', 'disabled');
    }
  },
  sendFeedback: function() {
    var that = this;
    $.ajax({
      type: 'POST',
      url: SERVER + 'app/' + this.id + '/' + this.$('select[name=type]').val() + '/feedback',
      data: { feedback: this.$('textarea[name=feedback]').val() },
      dataType: 'json',
      timeout: 300,
      context: $('body'),
      success: function(){
        that.$('textarea[name=feedback]').val('');
        that.validateForm.call(that);
        that.feedbacks.fetch({
          success: function() { that.render.call(that); }
        });
      },
      error: function(){
        alert('Ajax error!');
      }
    });
    return false;
  }
});

//////
// Router
//////
var AppRouter = Backbone.Router.extend({
  current: undefined,
  routes: {
    '': 'index',
    'app/:id' : 'app'
  },
  index: function() {
    this.show(new AppsView());
  },
  app: function(id) {
    this.show(new AppView({id: id}));
  },
  show: function(view) {
    if (this.current && this.current.destroy) {
      this.current.destroy();
    }
    this.current = view;
  }
});
window.AppRouter = new AppRouter();
Backbone.history.start();
})();
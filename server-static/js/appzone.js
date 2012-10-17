/*global Backbone,_ */
(function() {
var SERVER = 'http://appzone-api.pes.ch/';
var AppItem = Backbone.Model.extend({
  url: function() { return SERVER + 'app/' + this.id; },
  clear: function() {
    this.destroy();
  }
});

var AppItemList = Backbone.Collection.extend({
  model: AppItem,
  url: SERVER + 'apps'
});

var AppItemView = Backbone.View.extend({
  tagName:  'li',
  template: _.template($('#app-template').html()),
  render: function() {
    var app = this.model;
    this.$el.html(this.template(app.toJSON()));
    if (app.attributes.android) {
      this.$('.android a').attr('href', SERVER + 'app/' + app.id + '/android');
    }
    if (app.attributes.ios) {
      this.$('.ios a').attr('href', SERVER + 'app/' + app.id + '/ios');
    }
    return this;
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
  }
});

var AppView = Backbone.View.extend({
  el: $('#appzoneapp'),
  apps: new AppItemList(),
  feedbacks: new FeedbackList(),
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
    this.current = new AppsView();
  },
  app: function(id) {
    this.current = new AppView({id: id});
  },
  initialize: function() {
    this.on('all', function(routeEvent) {
      $('#app-list').children().remove();
      $('#feedbacks').children().remove();
    });
  }
});
window.AppRouter = new AppRouter();
Backbone.history.start();
})();
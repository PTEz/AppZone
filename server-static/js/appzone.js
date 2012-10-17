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
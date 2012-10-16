/*global Backbone,_ */
(function() {
var SERVER = 'http://172.19.37.164:8080/';
var AppItem = Backbone.Model.extend({
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
  initialize: function() {
  },
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

//////
// App
//////
var Apps = new AppItemList();
var AppView = Backbone.View.extend({
  el: $('#appzoneapp'),
  initialize: function() {
    Apps.fetch({
      success: this.render
    });
  },
  render: function() {
    Apps.each(function(app) {
      var view = new AppItemView({model: app});
      this.$('#app-list').append(view.render().el);
    });
  }
});
window.App = new AppView();
})();
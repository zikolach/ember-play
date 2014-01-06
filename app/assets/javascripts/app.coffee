$(document).foundation()

window.App = App = Em.Application.create
    LOG_TRANSITIONS: true
    ready: ->
        this.register 'controller:alert', App.AlertController, {singleton: true}
        this.inject 'route', 'alert', 'controller:alert'
        return

App.Router.map ->
    @route 'register'
    @route 'login'
    @route 'logout'
    @resource 'profile', ->
        @route 'show'
        return
    @route 'terms'
    return

App.AlertController = Em.Controller.extend
    message: window.message
    messageClass: (->
        message = @get 'message'
        if message?.match /success/gi
            'success'
        else
            'warning'
    ).property 'message'
    show: (message) ->
        @set 'message', message
        return
    actions:
        close: ->
            @set 'message', ''
            return

App.ApplicationAdapter = DS.RESTAdapter.extend
    namespace: 'api/v1'

# auth
App.Auth = Em.Auth.extend
    request:            'jquery'
    response:           'json'
    strategy:           'token'
    tokenKey:           'token'
    tokenIdKey:         'userId'
    tokenLocation:      'customHeader'
    tokenHeaderKey:      'token'
    session:            'cookie'
    signInEndPoint:     '/api/v1/login'
    signOutEndPoint:    '/api/v1/logout'
    modules: ['emberData', 'rememberable', 'authRedirectable', 'actionRedirectable']
    emberData:
        userModel:      'user'
    rememberable:
        tokenKey:       'remember_token'
        period:         14
        autoRecall:     true
    authRedirectable:
        route:          'login'
    actionRedirectable:
        signInRoute:    'profile'
        #signInSmart:    true
        signInBlacklist: ['register', 'login']
        signOutRoute:   'index'


App.ApplicationView = Em.View.extend
    didInsertElement: ->
        $("body").on "click.toggleCanvas", ".off-canvas-list a", -> $(".exit-off-canvas").click()
        return


App.IndexRoute = Ember.Route.extend()

$.postJSON = (url, data, callback) ->
    return jQuery.ajax
        type: 'POST'
        url: url
        contentType: 'application/json'
        data: JSON.stringify(data)
        dataType: 'json'
        success: callback
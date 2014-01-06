App.RegisterRoute = Em.Route.extend
    setupController: (controller) ->
        controller.setProperties
            email: ''
            password: ''
            confirmation: ''
    actions:
        register: ->
            self = @
            data = @controller.getProperties ['email', 'password', 'confirmation']
            if data.password isnt data.confirmation
                self.alert.show 'Password and confirmation must be equal'
            else
                delete data['confirmation']
                $.postJSON('/api/v1/register', data).then(
                    (res) ->
                        self.alert.show res.message
                        self.transitionTo 'login'
                        return
                    (err) ->
                        self.alert.show err.responseJSON.message
                        return
                )
            return
        cancel: ->
            @transitionTo 'index'

App.LoginRoute = Em.Route.extend
    setupController: (controller) ->
        controller.setProperties
            email: ''
            password: ''
    actions:
        login: ->
            self = @
            data = @controller.getProperties(['email', 'password']);
            rememberMe = @controller.get('rememberMe');
            @auth.signIn({data : data}).then(
                (res) ->
                    self.auth.get('module.rememberable').forget() if not rememberMe
                    self.alert.show res.message
                (err) ->
                    self.alert.show err.message
            )
        cancel: ->
            @transitionTo 'index'


App.LogoutRoute = Em.Route.extend
    authRedirectable: true
    actions:
        logout: ->
            self = @
            @auth.signOut().then(
                (res) ->
                    self.store.unloadAll 'user'
                    self.alert.show res.message
                (err) ->
                    self.alert.show err.responseJSON.message
            )

App.ProfileRoute = Em.Route.extend
    authRedirectable: true
    model: ->
        console.log @auth.get('user')
        @store.find 'user', @auth.get('userId')
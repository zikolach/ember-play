App.User = DS.Model.extend
    email: DS.attr 'string'
    password: DS.attr 'string'
    #name: (-> @get('screenName') | @get('email')).property(['email', 'screenName'])
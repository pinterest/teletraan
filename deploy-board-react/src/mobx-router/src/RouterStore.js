import {autorun, observable, computed, runInAction, toJS} from 'mobx'
import page from './page'
import _ from 'lodash'
import queryString from 'query-string'

export class RouterStore {
  @observable params = {}
  @observable queryParams = {}
  @observable currentRoute
  @observable historyChanged = 0
  hashbang = false
  replace = false
  context = {}
  routes = {}
  _routeIDs = {}
  _lastBeforeActivateFn = null
  _lastBeforeDeactivateFn = null
  _browserHistorySupport = !!(window.history && window.history.pushState)

  constructor({routes, context = {}, base = '', hashbang = false}) {
    this.base = base
    this.hashbang = hashbang
    this.context = context
    this._buildRoutes(routes)
    this._generatePagejsConfig()
    this.go = this.go.bind(this)
  }

  go(arg) {
    let pathOrID
    if (typeof arg === 'string') {
      pathOrID = arg
      arg = {}
    } else {
      let {to} = arg
      pathOrID = to
    }
    let to = this.getRoute(pathOrID)
    if (to) {
      arg = {...arg, to}
      this._go(arg)
    } else {
      throw new Error('Route ' + pathOrID + ' not found')
    }
  }

  getRoute(pathOrID) {
    if (this.routes[pathOrID]) {
      return this.routes[pathOrID]
    }
    if (this._routeIDs[pathOrID]) {
      return this._routeIDs[pathOrID]
    }
  }

  setContext() {
    if (arguments.length === 1) {
      Object.assign(this.context, arguments[0])
    } else if (arguments.length === 2) {
      let key = arguments[0],
        val = arguments[1]
      this.context[key] = val
    } else {
      throw new Error('No more than 2 arguments')
    }
  }

  getContext(key, defaultVal) {
    if (arguments.length === 0) return this.context
    if (key in this.context) return this.context[key]
    return defaultVal
  }

  deleteContext(key) {
    delete this.context[key]
  }

  start() {
    page.start({
      click: false, // bind to click events [true]
      popstate: true, // bind to popstate [true]
      dispatch: true, // perform initial dispatch [true]
      hashbang: this.hashbang, // add #! before urls [false]
      decodeURLComponents: true, // remove URL encoding from path components (query string, pathname, hash) [true]
    })
    //autorun and watch for path changes
    autorun(() => {
      let currentURL = this.currentURL()
      // console.log(`browserURL = "${browserURL}"`);
      // console.log(`currentURL() = "${currentURL()}"`);
      if (currentURL !== RouterStore.browserURL()) {
        // console.log(`pushing "${currentURL()}"`);
        if (this._browserHistorySupport) {
          if (this.replace) {
            window.history.replaceState(null, null, currentURL)
          } else {
            window.history.pushState(null, null, currentURL)
          }
          this.historyChanged++
        } else {
          console && console.warn("your browser doesn't support html5 history")
          window.location.href = currentURL
        }
      }
    })
  }

  static browserURL({queryParams = true} = {}) {
    if (queryParams) return window.location.pathname + window.location.search + window.location.hash
    else return window.location.pathname + window.location.hash
  }

  currentURL({queryParams = true} = {}) {
    if (this.currentRoute) {
      let q = queryParams ? this.queryParams : {}
      return this._getHREF(this.currentRoute.path, this.params, q)
    } else {
      return ''
    }
  }

  getHREF({to, params, queryParams}) {
    let r = this.getRoute(to)
    if (r) {
      return this._getHREF(r.path, params, queryParams)
    } else {
      throw new Error('Route ' + to + ' not found')
    }
  }

  get routesArray() {
    return Object.entries(this.routes).map(([k, v]) => v)
  }

  getCurrentRouteState() {
    if (!this.currentRoute) {
      return null
    }
    return {
      to: this.currentRoute.path,
      params: toJS(this.params),
      queryParams: toJS(this.queryParams),
    }
  }

  _convertArrayRoutes(routes) {
    let starRoute, defaultRoute
    let res = {}
    routes.forEach(route => {
      res[route.path] = route
      if (route.path === '*') starRoute = route
      else if (route.defaultPath) defaultRoute = route.path
    })
    if (!starRoute && !_.isNil(defaultRoute)) {
      res['*'] = {
        path: '*',
        beforeActivate({router}) {
          router.go(defaultRoute)
        },
      }
    }
    return res
  }

  _buildRoutes(routes) {
    let router = this
    if (Array.isArray(routes)) {
      routes = this._convertArrayRoutes(routes)
    } else {
      routes = {...routes}
    }
    for (let path of Object.keys(routes)) {
      let route = routes[path]
      if (typeof route === 'string') {
        routes[path] = (function(redirect) {
          return {
            path,
            beforeActivate() {
              router.go(redirect)
            },
          }
        })(route)
      } else {
        route.path = path
        if (route.id) this._routeIDs[route.id] = route
      }
    }
    this.routes = routes
  }

  _generatePagejsConfig() {
    if (this.base) {
      page.base(this.base)
    }
    for (let path of Object.keys(this.routes)) {
      const route = this.routes[path]
      ;((route, path) =>
        page(path, (ctx, next) => {
          const queryParams = queryString.parse(window.location.search)
          // console.log(`page callback for path "${path}"`);
          // console.log(window.location);
          // console.log(ctx);
          // console.warn(`Entering route ${route.path}`);
          let params = ctx.params
          // wildcard route is different that there's no named argument
          // for now just return the whole match as a string
          if (path === '*' && params && params[0]) params = params[0]
          this._go({to: route, params, queryParams})
          // next(); we don't want the catchall '*' route to execute unless its the first match
        }))(route, path)
    }
  }

  static _serializeQueryParams(queryParams = {}) {
    const jsQueryParams = toJS(queryParams)
    const queryParamsString = queryString.stringify(jsQueryParams).toString()
    return queryParamsString ? '?' + queryParamsString : ''
  }

  static _getRegexMatches(string, regexExpression, callback) {
    let match
    while ((match = regexExpression.exec(string)) !== null) {
      callback(match)
    }
  }

  /*
     replaces url params placeholders with params from an object
     Example: if url is /book/:id/page/:pageId and object is {id:100, pageId:200} it will return /book/100/page/200
     */
  static _serializeParams(routeSpec, params) {
    const jsParams = toJS(params)
    if (routeSpec === '*') {
      return jsParams.toString()
    }
    const paramRegex = /\/(:([^/?]*)\??)/g
    let newPath = routeSpec
    RouterStore._getRegexMatches(
      routeSpec,
      paramRegex,
      // eslint-disable-next-line
      ([fullMatch, paramKey, paramKeyWithoutColon]) => {
        const value = jsParams[paramKeyWithoutColon]
        newPath = value ? newPath.replace(paramKey, value) : newPath.replace(`/${paramKey}`, '')
      }
    )
    return newPath.toString()
  }

  _getHREF(path, params, queryParams) {
    let main = RouterStore._serializeParams(path, params)
    let qstr = queryParams === true ? window.location.search : RouterStore._serializeQueryParams(queryParams || {})
    if (this.hashbang) {
      if (qstr) return `${this.base}${qstr}#!${main}`
      else return `${this.base || '/'}#!${main}`
    } else {
      return this.base + main + qstr
    }
  }

  _go({to, params = {}, queryParams = {}, replace = false}) {
    const routeChanged = !this.currentRoute || this.currentRoute.path !== to.path
    const oldParams = toJS(this.params)
    const oldQueryParams = toJS(this.queryParams)

    params = toJS(params)
    if (queryParams === true) queryParams = queryString.parse(window.location.search)
    else queryParams = toJS(queryParams || {})
    const paramsChanged = !_.isEqual(params, oldParams)
    const queryParamsChanged = !_.isEqual(queryParams, oldQueryParams)

    if (!(routeChanged || paramsChanged || queryParamsChanged)) {
      return
    }

    let commonArgs = {
      router: this,
      context: this.context,
      routeChanged,
      paramsChanged,
      queryParamsChanged,
      oldRoute: this.currentRoute && this.currentRoute.path,
      newRoute: to.path,
      oldParams,
      newParams: params,
      oldQueryParams,
      newQueryParams: queryParams,
    }
    let currentBeforeDeactivate = {
      to: this.currentRoute && this.currentRoute.path,
      fn: this.currentRoute && this.currentRoute.beforeDeactivate,
      params: oldParams,
      queryParams: oldQueryParams,
    }
    let currentBeforeActivate = {
      to: to.path,
      fn: to.beforeActivate,
      params: oldParams,
      queryParams: oldQueryParams,
    }

    const completeTransaction = () => {
      this._lastBeforeActivateFn = currentBeforeActivate
      runInAction(() => {
        this.replace = replace
        this.currentRoute = to
        this.params = params
        this.queryParams = queryParams
      })
    }

    const continueBeforeActivate = () => {
      this._lastBeforeDeactivateFn = currentBeforeDeactivate

      if (to.beforeActivate && !_.isEqual(this._lastBeforeActivateFn, currentBeforeActivate)) {
        to.beforeActivate(Object.assign({next: completeTransaction}, commonArgs))
      } else {
        this._lastBeforeActivateFn = null
        completeTransaction()
      }
    }

    if (!this.currentRoute) {
      continueBeforeActivate()
    } else {
      if (this.currentRoute.beforeDeactivate && !_.isEqual(this._lastBeforeDeactivateFn, currentBeforeDeactivate)) {
        this.currentRoute.beforeDeactivate(Object.assign({next: continueBeforeActivate}, commonArgs))
      } else {
        continueBeforeActivate()
      }
    }
  }
}

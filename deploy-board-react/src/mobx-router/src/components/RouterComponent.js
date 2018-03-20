// eslint-disable-next-line
import React from 'react'
import {observer, inject} from 'mobx-react'
import PropTypes from 'prop-types'

const RouterComponentBase = props => {
  const {router, group = 'component'} = props
  if (!router) {
    throw new Error('router needs to be defined')
  }
  if (router.currentRoute && group in router.currentRoute) {
    let comp = router.currentRoute[group]
    if (!comp || typeof comp === 'object') {
      return comp
    } else {
      let props = Object.assign({router, route: router.currentRoute}, props)
      return React.createElement(comp, props)
    }
  } else {
    console &&
      console.warn(`group '${group}' is not defined in route '${router.currentRoute && router.currentRoute.path}'`)
    return null
  }
}

const RouterComponent = inject('router')(observer(RouterComponentBase))

RouterComponent.propTypes = {
  group: PropTypes.string,
}

export {RouterComponent}

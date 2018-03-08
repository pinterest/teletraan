// eslint-disable-next-line
import React from 'react'
import PropTypes from 'prop-types'
import {observer, inject} from 'mobx-react'
import _ from 'lodash'

// specialReactKeys = { children: true, key: true, ref: true }

const LinkBase = props => {
  const {to, params = {}, queryParams = {}, replace = false, refresh = false, innerRef, router, children} = props

  if (!router || !to) {
    let propname = !router ? 'router' : 'to'
    console && console.error(`The '${propname}' prop must be defined for a Link component to work`)
    return null
  }

  const innerProps = _.omit(props, [
    'to',
    'params',
    'queryParams',
    'replace',
    'refresh',
    'innerRef',
    'ref',
    'router',
    'children',
  ])

  const onClick = e => {
    const middleClick = e.button === 2
    const cmdOrCtrl = e.metaKey || e.ctrlKey
    const openinNewTab = middleClick || cmdOrCtrl
    const shouldNavigateManually = refresh || openinNewTab || cmdOrCtrl

    if (props.onClick) props.onClick(e)
    if (!shouldNavigateManually) {
      if (!e.defaultPrevented) e.preventDefault()
      router.go({to, params, queryParams, replace})
    }
  }

  return (
    <a {...innerProps} ref={innerRef} href={router.getHREF({to, params, queryParams})} onClick={onClick}>
      {children}
    </a>
  )
}

const Link = inject('router')(observer(LinkBase))

Link.propTypes = {
  onClick: PropTypes.func,
  target: PropTypes.string,
  replace: PropTypes.bool,
  refresh: PropTypes.bool,
  to: PropTypes.string.isRequired,
  params: PropTypes.object,
  queryParams: PropTypes.oneOfType([PropTypes.object, PropTypes.bool]),
  innerRef: PropTypes.oneOfType([PropTypes.string, PropTypes.func]),
}

export {Link}

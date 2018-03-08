import React from 'react'
import PropTypes from 'prop-types'
import {Link} from './Link'
import {RouterStore} from '../RouterStore'
import {observer, inject} from 'mobx-react'

/**
 * A <Link> wrapper that knows if it's "active" or not.
 */
class NavLinkBase extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
    this.onpopstate = this.onpopstate.bind(this)
  }

  onpopstate(e) {
    this.setState({})
  }

  componentDidMount() {
    window.addEventListener('popstate', this.onpopstate)
  }

  componentWillUnmount() {
    window.removeEventListener('popstate', this.onpopstate)
  }

  render() {
    const {
      router,
      to,
      exact = false,
      params = {},
      queryParams = {},
      activeClassName,
      className,
      activeStyle,
      style,
      isActive: getIsActive,
      ...rest
    } = this.props

    const isActive = (() => {
      let c = router.historyChanged || true
      if (c) {
        if (getIsActive) return !!getIsActive(/*match, location*/)
        let url = RouterStore.browserURL({queryParams: exact})
        let href = router.getHREF({to, params, queryParams: exact ? queryParams : {}})
        // console.log(`url ${url} href ${href}`)
        return url === href
      }
    })()

    return (
      <Link
        router={router}
        to={to}
        params={params}
        queryParams={queryParams}
        className={isActive ? [className, activeClassName].filter(i => i).join(' ') : className}
        style={isActive ? {...style, ...activeStyle} : style}
        {...rest}
      />
    )
  }
}

const NavLink = inject('router')(observer(NavLinkBase))

NavLink.propTypes = {
  to: Link.propTypes.to,
  exact: PropTypes.bool,
  // strict: PropTypes.bool,
  activeClassName: PropTypes.string,
  className: PropTypes.string,
  activeStyle: PropTypes.object,
  style: PropTypes.object,
  isActive: PropTypes.func,
}

NavLink.defaultProps = {
  activeClassName: 'active',
}

export {NavLink}

import React from 'react'
import {Home} from './components/Home'
import {AllEnv} from './components/AllEnvirons'
import {EnvLanding} from './components/Environ'
import {EnvBuild} from './components/EnvironBuild'
import {PodView} from './components/Pod'

const routes = {
  '/': {
    id: 'routeHome',
    component: <Home />
  },
  '/envs': {
    id: 'routeAllEnvs',
    component: <AllEnv />,
    beforeActivate({next, context: {envModel}}) {
      envModel.fetchAllEnvs()
      next()
    }
  },
  '/envs/:env': {
    id: 'routeEnv',
    component: <EnvLanding />,
    beforeActivate({router, next, newParams, context: {envModel}}) {
      envModel.fetchEnv(newParams.env, newParams.stage)
      next()
    }
  },
  '/envs/:env/:stage': {
    id: 'routeEnvStage',
    intervalID: null,
    component: <EnvLanding />,
    beforeActivate({next, newParams, context: {envModel}}) {
      envModel.fetchEnv(newParams.env, newParams.stage)
      this.intervalID = setInterval(() => {
        envModel.fetchEnvDeployProgress(newParams.env, newParams.stage)
      }, 30000) // refresh every 30 sec
      next()
    },
    beforeDeactivate({next}) {
      if (this.intervalID) {
        clearInterval(this.intervalID)
        this.intervalID = null
      }
      next()
    }
  },
  '/envs/:env/:stage/new_deploy': {
    id: 'routeNewDeploy',
    component: <EnvBuild />,
    beforeActivate({next, newParams, context: {envModel}}) {
      envModel.fetchEnvBuilds(newParams.env)
      next()
    }
  },
  '/envs/:env/:stage/pods/:podName': {
    id: 'routeEnvPod',
    component: <PodView />,
    async beforeActivate({next, newParams, context: {podModel}}) {
      await podModel.fetchPod(newParams.env, newParams.stage, newParams.podName)
      next()
    }
  },

  '*': '/'
}

export {routes}

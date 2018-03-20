import {observable, runInAction} from 'mobx'
import {APIs} from '../apis'

export class EnvModel {
  @observable.ref allEnvs = observable.shallowArray([])
  @observable.ref env = observable.shallowMap({})
  @observable.ref pods = observable.shallowMap({})
  @observable.ref replicaSets = observable.shallowMap({})
  @observable.ref deploy = observable.shallowMap({})
  @observable.ref build = observable.shallowMap({})
  @observable.ref builds = observable.shallowMap({})

  async fetchAllEnvs() {
    let res
    try {
      res = await APIs.allEnvs()
    } catch (err) {
      console.error(err)
      res = []
    }
    runInAction(() => (this.allEnvs = res))
  }

  async fetchEnvBuilds(envName) {
    let env = await APIs.getEnv(envName)
    if (!env) {
      return
    }
    runInAction(() => this.env.set(envName, env))
    let builds
    try {
      builds = await APIs.getEnvBuilds(env.buildName, 30)
      runInAction(() => this.builds.set(envName, builds))
    } catch (err) {
      console.error(err)
    }
  }

  async fetchEnvDeployProgress(envName, stageName) {
    let progress
    let key = [envName, stageName].join(',')
    let env = this.env.get(key)
    if (!env) {
      return
    }
    try {
      progress = await APIs.getProgress(envName, stageName, env.k8sClusterName)
      if (progress) {
        runInAction(() => {
          this.pods.set(key, progress.pods)
          this.replicaSets.set(key, progress.replicaSets)
        })
      }
    } catch (err) {
      console.error(err)
    }
  }

  async fetchEnv(envName, stageName) {
    // get env
    let env, key
    try {
      env = await APIs.getEnv(envName, stageName)
      if (!env) {
        return
      }
      key = [env.envName, env.stageName].join(',')
      this.env.set(key, env)
    } catch (err) {
      console.error(err)
      return
    }

    // get env deploy
    stageName = env.stageName

    let deploy
    try {
      deploy = await APIs.getDeploy(envName, stageName, env.k8sClusterName)
      runInAction(() => {
        this.deploy.set(key, deploy)
      })
    } catch (err) {
      console.error(err)
      return
    }

    // get env build
    let build
    try {
      if (deploy.buildId) {
        build = await APIs.getBuild(deploy.buildId)
        runInAction(() => {
          this.build.set(key, build)
        })
      }
    } catch (err) {
      console.error(err)
    }

    // get env recent builds
    let builds
    try {
      builds = await APIs.getEnvBuilds(env.buildName, 3)
      runInAction(() => {
        this.builds.set(key, builds)
      })
    } catch (err) {
      console.error(err)
      return
    }

    // get env deploy progress
    await this.fetchEnvDeployProgress(env.envName, env.stageName)
  }
}

import {APIs} from '../apis'
import {observable, runInAction} from 'mobx'

export class PodModel {
  @observable.ref pod = observable.shallowMap({})

  async fetchPod(envName, stageName, podName) {
    let res
    try {
      res = await APIs.getPod(podName)
      runInAction(() => this.pod.set(podName, res))
    } catch (err) {
      console.error(err)
    }
  }
}

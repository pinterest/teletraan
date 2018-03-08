import {trackAsync} from './utils/TrackAsync'

const ARGONATH_DOMAIN = process.env.ARGONATH_DOMAIN
const TELETRAAN_TOKEN = process.env.TELETRAAN_TOKEN
const TELETRAAN_DOMAIN = process.env.TELETRAAN_DOMAIN
let CALL_REMOTE_APIS = process.env.CALL_REMOTE_APIS
CALL_REMOTE_APIS = !!(CALL_REMOTE_APIS ? CALL_REMOTE_APIS.replace(/\s+/g, '') : false);

export const APIs = {
  @trackAsync
  async allEnvs() {
    // let delay = Math.random() * 5;
    // await sleep(delay * 1000); // simulate data is pending from remote
    let data = await fetch('/data.json')
    let response = await data.json()
    return response.envs
  },

  @trackAsync
  async getEnv(envName, stageName) {
    const isMatch = p => p.envName === envName
    return (await this.allEnvs()).find(isMatch)
  },

  @trackAsync
  async newDeploy(envName, stageName, k8sClusterName, build) {
    let url = `envs/${envName}/${stageName}/deploy`
    console.log('url', ARGONATH_DOMAIN + url)

    let response = await fetch(ARGONATH_DOMAIN + url, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        envName: envName,
        stageName: stageName,
        telefig: build.telefig,
        buildId: build.id,
        deployClusterName: k8sClusterName
      })
    })
    return await response.json()
  },

  @trackAsync
  async getProgress(envName, stageName, k8sClusterName) {
    if (CALL_REMOTE_APIS) {
      let url = `envs/${envName}/${stageName}/deploy/progress?deployClusterName=${k8sClusterName}`
      console.log('url', ARGONATH_DOMAIN + url)
      let response = await fetch(ARGONATH_DOMAIN + url, {
        method: 'PUT',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json'
        }
      })
      return await response.json()
    } else {
      // local
      let response = await fetch('/data.json');
      return await response.json();
    }
  },

  @trackAsync
  async getDeploy(envName, stageName, k8sClusterName) {
    if (CALL_REMOTE_APIS) {
      let url = `envs/${envName}/${stageName}/current?deployClusterName=${k8sClusterName}`
      console.log('url', ARGONATH_DOMAIN + url)
      let response = await fetch(ARGONATH_DOMAIN + url, {
        method: 'GET',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json'
        }
      })
      if (response.ok) {
        return await response.json()
      }
      return {}
    } else {
      let response = await fetch('/data.json');
      let responseJson = await response.json();
      return responseJson.deploy;
    }
  },

  @trackAsync
  async getBuild(buildId) {
    if (CALL_REMOTE_APIS) {
      let url = `builds/${buildId}`
      console.log('url', ARGONATH_DOMAIN + url)
      let response = await fetch(TELETRAAN_DOMAIN + url, {
        method: 'GET',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          Authorization: `token ${TELETRAAN_TOKEN}`
        }
      })
      return await response.json()
    } else {
      let response = await fetch('/data.json');
      let responseJson = await response.json();
      return responseJson.build;
    }
  },

  @trackAsync
  async getEnvBuilds(buildName, limit) {
    if (CALL_REMOTE_APIS) {
      let url = `builds/tags?name=${buildName}&pageIndex=1&pageSize=${limit}`
      console.log('url', ARGONATH_DOMAIN + url)
      let response = await fetch(TELETRAAN_DOMAIN + url, {
        method: 'GET',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          Authorization: `token ${TELETRAAN_TOKEN}`
        }
      })
      return await response.json()
    } else {
      let response = await fetch('/data.json');
      let responseJson = await response.json();
      return responseJson.builds;
    }
  },

  @trackAsync
  async getPod(podName) {
    if (CALL_REMOTE_APIS) {
      let url = `pods/${podName}`
      console.log('url', ARGONATH_DOMAIN + url)
      let response = await fetch(ARGONATH_DOMAIN + url, {
        method: 'GET',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json'
        }
      })
      return await response.json()
    } else {
      let response = await fetch('/data.json');
      let responseJson = await response.json();
      return responseJson.pod;
    }
  }
}

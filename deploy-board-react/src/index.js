import React from 'react'
import ReactDOM from 'react-dom'
import {Provider} from 'mobx-react'
import {RouterStore, RouterComponent} from './mobx-router/src'
import {EnvModel} from './models/EnvModel'
import {PodModel} from './models/PodModel'
import {useStrict} from 'mobx'
import {APIs} from './apis'
import {routes} from './routes'
import 'antd/dist/antd.css'
import './index.css'

useStrict(true) // disallow modifying observable outside of action()/runInAction()

const envModel = new EnvModel()
const podModel = new PodModel()
const router = new RouterStore({routes, context: {envModel, podModel}})
router.start()
ReactDOM.render(
  <Provider router={router} envModel={envModel} podModel={podModel} APIs={APIs}>
    <RouterComponent />
  </Provider>,
  document.getElementById('root')
)

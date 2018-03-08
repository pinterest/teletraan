import React from 'react'
import {Menu, Button, Breadcrumb, Alert, Spin, Divider} from 'antd'
import {inject, observer} from 'mobx-react/index'
import {BaseLayout} from '../layouts/BaseLayout'

@inject('podModel', 'router')
@observer
export class PodDetailView extends React.Component {
  render() {
    const {podModel, router} = this.props
    let podName = router.params.podName
    let pod = podModel.pod.get(podName)
    if (!pod) {
      return (
        <Spin tip="Loading...">
          <Alert message="Fetching pod" description="Please wait" type="info" />
        </Spin>
      )
    }
    let conditions = JSON.parse(pod.conditions)
    console.log(pod)

    const renderRows = rows => {
      //const style = {verticalAlign: 'top', whiteSpace: 'nowrap', padding: '8px 10px 6px 0px'}
      return rows.map(([label, content]) => (
        <tr key={label}>
          <th className="table-label">{label}</th>
          <td>{content}</td>
        </tr>
      ))
    }

    return (
      <div>
        <div className="panel-body">
          <table className="table">
            <tbody>
              {renderRows([
                ['Name:', pod.podName],
                ['Namespace:', pod.namespace],
                [
                  'Labels:',
                  Object.entries(pod.labels).map(([name, value]) => (
                    <div className="row" key={name}>
                      <div>
                        {name}: {value}
                      </div>
                    </div>
                  ))
                ],
                [
                  'Annotations:',
                  Object.entries(pod.annotations).map(([name, value]) => {
                    if (name === 'kubernetes.io/created-by') {
                      value = JSON.parse(value)
                      return (
                        <div className="row" key={name}>
                          <div>
                            {name}: <a type="button">{value.reference.kind}</a>
                            <Divider type="vertical" />
                            <a type="button">{value.reference.name}</a>
                          </div>
                        </div>
                      )
                    }
                    return (
                      <div className="row" key={name}>
                        <a type="button">
                          {name}: {value}
                        </a>
                      </div>
                    )
                  })
                ],
                ['Creation Time:', pod.createTime],
                ['Status:', pod.phase],
                ['Node:', pod.node],
                ['IP:', pod.ip]
              ])}
            </tbody>
          </table>
        </div>
        <div className="panel-body">
          <table className="table">
            <tbody>
              <tr>
                <th className="table-label">Type</th>
                <th className="table-label">Status</th>
                <th className="table-label">Last transition time</th>
                <th className="table-label">Reason</th>
                <th className="table-label">Message</th>
              </tr>
              {conditions.map((condition, index) => (
                <tr key={index}>
                  <td>{condition.type}</td>
                  <td>{condition.status}</td>
                  <td>{condition.lastTransitionTime.Time}</td>
                  <td>{condition.reason}</td>
                  <td>{condition.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    )
  }
}

@inject('podModel', 'router')
@observer
export class PodBreadcrumb extends React.Component {
  render() {
    const {router: {go}} = this.props
    return (
      <Breadcrumb style={{margin: '16px 0'}}>
        <Breadcrumb.Item>
          <a onClick={() => go('routeHome')}>Home</a>
        </Breadcrumb.Item>
        <Breadcrumb.Item>
          <a onClick={() => go('routeAllEnvs')}>Environments</a>
        </Breadcrumb.Item>
        <Breadcrumb.Item>
          <a
            onClick={() =>
              go({
                to: 'routeEnvStage',
                params: {
                  env: this.props.router.params.env,
                  stage: this.props.router.params.stage
                }
              })
            }>
            {this.props.router.params.env}({this.props.router.params.stage})
          </a>
        </Breadcrumb.Item>
        <Breadcrumb.Item>
          <a onClick={() => go('routeEnvPods')}>pods</a>
        </Breadcrumb.Item>
        <Breadcrumb.Item>{this.props.router.params.podName}</Breadcrumb.Item>
      </Breadcrumb>
    )
  }
}

@inject('podModel', 'router')
@observer
export class PodSidebar extends React.Component {
  render() {
    return (
      <Menu mode="inline" style={{height: '100%'}}>
        <Menu.Item key="podAction">
          <Button title="Gracefully shutdown the service and terminate the pod.">Terminate Pod</Button>
        </Menu.Item>
      </Menu>
    )
  }
}

@inject('podModel', 'router')
@observer
export class PodView extends React.Component {
  render() {
    return (
      <BaseLayout
        children={{
          main: <PodDetailView />,
          sidebar: <PodSidebar />,
          breadcrumb: <PodBreadcrumb />
        }}
      />
    )
  }
}

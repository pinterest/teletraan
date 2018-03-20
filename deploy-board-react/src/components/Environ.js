import React from 'react'
import {Icon, Menu, Tabs, Table, Button, List, Row, Col, Divider, Breadcrumb, Tooltip, Spin, Alert} from 'antd'
import {observer, inject} from 'mobx-react'
import {BaseLayout} from '../layouts/BaseLayout'
import {FaClockO, FaCheck, FaExclamationTriangle} from 'react-icons/lib/fa'

@inject('envModel', 'router')
@observer
export class EnvLandingView extends React.Component {
  podPhases = ['failed', 'unknown', 'pending', 'running', 'succeeded']

  handleChange = (pagination, filters, sorter) => {
    console.log('Various parameters', pagination, filters, sorter)
  }

  rollbackDeploy = () => {
    console.log('rollback')
  }

  pauseDeploy = () => {
    console.log('pause')
  }

  listReplicaSetStatus(replicaSetName, replicaSets) {
    let replicaSet = replicaSets.find(item => {
      return item.name === replicaSetName
    })
    return (
      <Tooltip placement="topLeft" title={replicaSet.conditions} arrowPointAtCenter>
        <span>
          {replicaSet.currentReplicas} current / {replicaSet.desiredReplicas} desired
        </span>
      </Tooltip>
    )
  }

  listPodStatus(replicaSetName, pods) {
    let allStatus = {}
    this.podPhases.forEach(phase => {
      allStatus[phase] = 0
    })
    pods.forEach(pod => {
      let status = pod.phase.toString().toLowerCase()
      allStatus[status] += 1
    })
    return `${allStatus.running} Running / ${allStatus.pending} Waiting / ${allStatus.succeeded} Succeeded / ${
      allStatus.failed
    } Failed`
  }

  listPods(replicaSetName, pods) {
    // group by pods status in order: FAILED, UNKNOWN, PENDING, RUNNING, SUCCEEDED
    const {envModel, router} = this.props

    pods.forEach(pod => {
      if (pod.phase === 'FAILED') {
        pod.phaseOrder = 5
        pod.cls = 'danger'
      } else if (pod.phase === 'UNKNOWN') {
        pod.phaseOrder = 4
        pod.cls = 'danger'
      } else if (pod.phase === 'PENDING') {
        pod.phaseOrder = 3
        pod.cls = 'warning'
      } else if (pod.phase === 'RUNNING') {
        pod.phaseOrder = 2
      } else if (pod.phase === 'SUCCEEDED') {
        pod.phaseOrder = 1
      } else {
        pod.phaseOrder = 0
      }
    })
    return pods.sort((a, b) => a.phaseOrder < b.phaseOrder).map(function(pod) {
      let icon = <FaCheck />
      if (pod.phase === 'PENDING') {
        icon = <FaClockO />
      } else if (pod.phase === 'FAILED' || pod.phase === 'UNKNOWN') {
        icon = <FaExclamationTriangle />
      }

      let conditions = JSON.parse(pod.conditions)
      let cls = pod.cls || ''
      return (
        <Tooltip
          placement="topLeft"
          title={
            <List
              size="small"
              dataSource={conditions}
              renderItem={item => {
                if (item.message) {
                  return (
                    <List.Item className="pod status">
                      {item.type}({item.status}): {item.message}
                    </List.Item>
                  )
                }
                return (
                  <List.Item className="pod status">
                    {item.type}({item.status})
                  </List.Item>
                )
              }}
            />
          }
          arrowPointAtCenter
          key={pod.podName}>
          <Col className={'pod ' + cls}>
            <a href={'/envs/' + router.params.env + '/' + router.params.stage + '/pods/' + pod.podName}>
              {pod.podName} {icon}
            </a>
          </Col>
        </Tooltip>
      )
    })
  }

  render() {
    const {envModel, router} = this.props
    let key = [router.params.env, router.params.stage].join(',')

    let deploy = envModel.deploy.get(key)
    let build = envModel.build.get(key)
    let pods = envModel.pods.get(key)

    if (!deploy || !build || !pods) {
      return (
        <Spin tip="Loading...">
          <Alert message="Fetching environment" description="Please wait" type="info" />
        </Spin>
      )
    }

    let data = [{build: build, deploy: deploy, deployId: deploy.id}]

    let replicaSets = (envModel.replicaSets.get(key) || []).slice()
    const columns = [
      {
        title: 'Build',
        dataIndex: 'build',
        render: build => (
          <a title="Click to see the build details" href={'#/builds/' + build.id}>
            {build.branch}/{build.commitShort}
          </a>
        )
      },
      {
        title: 'Type',
        dataIndex: 'deploy.type'
      },
      {
        title: 'State',
        dataIndex: 'deploy.state'
      },
      {
        title: 'Progress',
        dataIndex: 'deploy',
        render: deploy => (
          <span>
            {deploy.successTotal}/{deploy.total}
          </span>
        )
      },
      {
        title: 'Elapsed',
        dataIndex: 'deploy.startDate',
        render: startDate => <span>{startDate}</span>
      },
      {
        title: 'Operator',
        dataIndex: 'deploy.operator'
      },
      {
        title: 'Details',
        dataIndex: 'deploy.id',
        render: deploy_id => <a href={'#/deploy/' + deploy_id}>view</a>
      }
    ]
    let stageName = router.params.stage

    if (stageName) {
      return (
        <div className="card-container">
          <Tabs type="card">
            <Tabs.TabPane tab={stageName} key="1">
              <div>
                <h3 style={{marginBottom: 16}}>Active Deployment</h3>

                <div className="table-operations" align="right">
                  <Button onClick={this.rollbackDeploy}>Rollback</Button>
                  <Button onClick={this.pauseDeploy}>Pause</Button>
                </div>
                <Table
                  className="panel"
                  size="middle"
                  columns={columns}
                  dataSource={data}
                  onChange={this.handleChange}
                  pagination={false}
                  rowKey="deployId"
                />
                <div className="panel">
                  {Object.entries(pods).map(([replicaSetName, pods]) => (
                    <div key={replicaSetName}>
                      <Row type="flex" justify="start" style={{padding: 15}} key="pods">
                        {this.listPods(replicaSetName, pods)}
                      </Row>
                      <div style={{fontSize: 12, paddingBottom: 5}}>
                        <Divider type="vertical" />
                        <span>
                          ReplicaSet: <a href={'#/rss/' + replicaSetName}>{replicaSetName}</a>
                        </span>
                        <Divider type="vertical" />
                        <span>{this.listReplicaSetStatus(replicaSetName, replicaSets)}</span>
                        <Divider type="vertical" />
                        <span>{this.listPodStatus(replicaSetName, pods)}</span>
                        <Divider type="vertical" />
                        <a href={'#/pods/' + replicaSetName}>All Details</a>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </Tabs.TabPane>
          </Tabs>
        </div>
      )
    } else {
      return <div />
    }
  }
}

@inject('envModel', 'router')
@observer
export class EnvBreadcrumb extends React.Component {
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
      </Breadcrumb>
    )
  }
}

@inject('envModel', 'router')
@observer
export class EnvSidebar extends React.Component {
  action = ({key, keyPath, item}) => {
    const {router: {go}} = this.props

    if (keyPath[keyPath.length - 1] === 'envAction') {
      console.log(item)
      console.log(keyPath)
      if (keyPath[0] === 'create') {
        go({
          to: 'routeNewDeploy',
          params: {
            env: this.props.router.params.env,
            stage: this.props.router.params.stage
          }
        })
      }
    }
  }

  render() {
    const {envModel, router} = this.props
    let key = [router.params.env, router.params.stage].join(',')
    let env = envModel.env.get(key) || {}
    let builds = envModel.builds.get(key) || []
    return (
      <Menu mode="inline" onClick={this.action} defaultOpenKeys={['envAction', 'buildAction']} style={{height: '100%'}}>
        <Menu.SubMenu
          key="envAction"
          title={
            <span>
              <Icon type="user" />Current Deploy
            </span>
          }>
          <Menu.Item key="create">Create Deploy</Menu.Item>
          <Menu.Item key="rollback">Rollback</Menu.Item>
          <Menu.Item key="history">Deploy History</Menu.Item>
          <Menu.Item key="configure">Configure</Menu.Item>
          <Menu.Item key="capacity">Capacity</Menu.Item>
        </Menu.SubMenu>
        <Menu.SubMenu
          key="buildAction"
          title={
            <span>
              <Icon type="notification" />Builds to Deploy
            </span>
          }>
          {builds.map((buildWithTag, index) => (
            <Menu.Item key={index}>
              <Button href={'/envs/' + env.envName + '/' + env.stageName + '/new_deploy'}>
                {buildWithTag.build.branch}/{buildWithTag.build.commitShort}
              </Button>
            </Menu.Item>
          ))}
          <Menu.Item key="more">
            <a href={'/envs/' + env.envName + '/' + env.stageName + '/new_deploy'}>More builds...</a>
          </Menu.Item>
        </Menu.SubMenu>
      </Menu>
    )
  }
}

@inject('envModel', 'router')
@observer
export class EnvLanding extends React.Component {
  render() {
    return (
      <BaseLayout
        children={{
          main: <EnvLandingView />,
          sidebar: <EnvSidebar />,
          breadcrumb: <EnvBreadcrumb />
        }}
      />
    )
  }
}

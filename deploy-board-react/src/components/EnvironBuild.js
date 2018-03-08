import React from 'react'
import {inject, observer} from 'mobx-react/index'
import {BaseLayout} from '../layouts/BaseLayout'
import {Breadcrumb, Menu, Icon, Table, Button} from 'antd'
import {toJS} from 'mobx'
import Timestamp from 'react-timestamp'
import {APIs} from '../apis'

@inject('envModel', 'router')
@observer
export class EnvBuildBreadcrumb extends React.Component {
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
          <a onClick={() =>
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
          <span>Create a deploy</span>
        </Breadcrumb.Item>
      </Breadcrumb>
    )
  }
}

@inject('envModel', 'router')
@observer
export class EnvBuildsView extends React.Component {
  async deployIt(build) {
    const {envModel, router: {go}} = this.props
    let env = envModel.env.get(this.props.router.params.env)
    let res
    try {
      // res = await envModel.newDeploy(env, build);
      res = await APIs.newDeploy(env.envName, env.stageName, env.k8sClusterName, build)
      console.log(res)
    } catch (err) {
      console.error(err, res)
    }
    go({
      to: '/envs/:env/:stage',
      params: {
        env: this.props.router.params.env,
        stage: this.props.router.params.stage
      }
    })
  }

  render() {
    const {envModel, router} = this.props
    let env = router.params.env
    let builds = toJS(envModel.builds.get(env) || [])

    const columns = [
      {
        title: 'Publish Date',
        dataIndex: 'build.publishDate',
        key: 'build.publishDate',
        render: text => (
          <a className="ant-dropdown-link">
            <Timestamp time={text / 1000} format="full" includeDay />
          </a>
        )
      },
      {
        title: 'Commit',
        dataIndex: 'build.commitShort',
        key: 'build.commitShort'
      },
      {
        title: 'Branch',
        dataIndex: 'build.branch',
        key: 'build.branch'
      },
      {
        title: 'Repo',
        dataIndex: 'build.repo',
        key: 'build.repo'
      },
      {
        title: 'Details',
        key: 'detail',
        render: (text, record) => <a href="#">View</a>
      }
    ]

    return (
      <div>
        <div>
          <h4 className="panel-title" align="left">
            Pick a build to deploy
          </h4>
        </div>
        <div>
          <Table
            columns={columns}
            dataSource={builds}
            expandRowByClick
            rowKey={record => record.build.id}
            expandedRowRender={record => (
              <p style={{margin: 0}}>
                <Button onClick={() => this.deployIt(record)}>Deploy it</Button>
              </p>
            )}
          />
        </div>
      </div>
    )
  }
}

@inject('envModel', 'router')
@observer
export class EnvBuildsSidebar extends React.Component {
  action = ({key, keyPath, item}) => {}

  render() {
    return (
      <Menu mode="inline" onClick={this.action} defaultOpenKeys={['envAction']} style={{height: '100%'}}>
        <Menu.SubMenu
          key="envAction"
          title={
            <span>
              <Icon type="user" />Action
            </span>
          }>
          <Menu.Item key="deploy">Current Deploy</Menu.Item>
        </Menu.SubMenu>
      </Menu>
    )
  }
}

@inject('envModel', 'router')
@observer
export class EnvBuild extends React.Component {
  render() {
    return (
      <BaseLayout
        children={{
          main: <EnvBuildsView />,
          sidebar: <EnvBuildsSidebar />,
          breadcrumb: <EnvBuildBreadcrumb />
        }}
      />
    )
  }
}

import React from 'react'
import {Menu, Breadcrumb, Icon, List} from 'antd'
import {observer} from 'mobx-react'
import {BaseLayout} from '../layouts/BaseLayout'
import {inject} from 'mobx-react/index'

const IconText = ({type, text}) => (
  <span>
    <Icon type={type} style={{marginRight: 8}} />
    {text}
  </span>
)

@inject('envModel', 'router')
@observer
export class AllEnvironsView extends React.Component {
  render() {
    const {envModel, router} = this.props
    const listData = envModel.allEnvs
    const pagination = {
      pageSize: 10,
      current: 1,
      total: listData.length,
      onChange: () => {}
    }
    return (
      <div>
        <h3 style={{marginBottom: 16}}>All Environments</h3>
        <List
          itemLayout="vertical"
          size="large"
          pagination={pagination}
          dataSource={listData}
          renderItem={item => (
            <List.Item
              key={item.id}
              actions={[
                <IconText type="user" text={'Last Operator: ' + item.lastOperator} />,
                <IconText type="schedule" text={'Last Update: ' + item.lastUpdate} />,
                <IconText type="message" text={'Chatroom: ' + item.chatroom} />
              ]}>
              <List.Item.Meta
                title={
                  <a
                    // href={ "/envs/" + item.envName + "/" + item.stageName}
                    onClick={() => {
                      router.go({to: 'routeEnvStage', params: {env: item.envName, stage: item.stageName}})
                    }}>
                    {item.envName} {item.stageName}
                  </a>
                }
                description={item.description}
              />
            </List.Item>
          )}
        />
      </div>
    )
  }
}

@inject('envModel', 'router')
@observer
export class AllEnvironsBreadcrumb extends React.Component {
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
      </Breadcrumb>
    )
  }
}

@inject('envModel', 'router')
@observer
export class AllEnvironsSidebar extends React.Component {
  render() {
    return (
      <Menu mode="inline" defaultOpenKeys={['sub1', 'sub2', 'sub3']} style={{height: '100%'}}>
        <Menu.SubMenu
          key="sub1"
          title={
            <span>
              <Icon type="user" />Environments
            </span>
          }>
          <Menu.Item key="1">Create Environment</Menu.Item>
          <Menu.Item key="2">All Deployments</Menu.Item>
        </Menu.SubMenu>
        <Menu.SubMenu
          key="sub3"
          title={
            <span>
              <Icon type="notification" />Recent Environments
            </span>
          }>
          <Menu.Item key="9">ngapp2</Menu.Item>
          <Menu.Item key="10">adminapp</Menu.Item>
          <Menu.Item key="11">deploy_board</Menu.Item>
          <Menu.Item key="12">rockstore</Menu.Item>
        </Menu.SubMenu>
      </Menu>
    )
  }
}

@inject('envModel', 'router')
@observer
export class AllEnv extends React.Component {
  render() {
    return (
      <BaseLayout
        children={{
          main: <AllEnvironsView />,
          sidebar: <AllEnvironsSidebar />,
          breadcrumb: <AllEnvironsBreadcrumb />
        }}
      />
    )
  }
}

import React from 'react'
import {Layout, Menu, Icon, Input, Dropdown, Row, Spin} from 'antd'
import {observer, inject} from 'mobx-react'
import {ASYNC_PENDING} from '../utils/TrackAsync'

const {Header, Content, Footer, Sider} = Layout

const userOptions = (
  <Menu>
    <Menu.Item>
      <a target="_blank" rel="noopener noreferrer" href="/logout">
        logout
      </a>
    </Menu.Item>
  </Menu>
)

@inject('envModel', 'router', 'APIs')
@observer
export class BaseLayout extends React.Component {
  render() {
    let spinning = ASYNC_PENDING.get()
    const {main, sidebar, breadcrumb} = this.props.children
    return (
      <Layout>
        <Header className="header">
          <div className="logo" />
          <Row type="flex" justify="end" align="middle">
            <Spin spinning={spinning} style={{marginRight: '10px'}} />
            <Menu theme="dark" mode="horizontal" defaultSelectedKeys={['1']} style={{lineHeight: '64px'}}>
              <Menu.Item key="allEnvirons"><a href='/envs/'>Environments</a></Menu.Item>
              <Menu.Item key="allBuilds"><a href='/builds/'>Builds</a></Menu.Item>
              <Menu.Item key="search">
                <Input placeholder="Search env..." />
              </Menu.Item>
              <Menu.Item key="help">Help</Menu.Item>
              <Menu.Item key="user">
                <Dropdown overlay={userOptions} placement="bottomLeft">
                  <a className="ant-dropdown-link">
                    Anonymous <Icon type="down" />
                  </a>
                </Dropdown>
              </Menu.Item>
            </Menu>
          </Row>
        </Header>
        <Content style={{padding: '0 50px'}}>
          {breadcrumb}
          <Layout style={{padding: '24px 0', background: '#fff'}}>
            <Sider width={200} style={{background: '#fff'}}>
              {sidebar}
            </Sider>
            <Content style={{padding: '0 24px', minHeight: 280}}>{main}</Content>
          </Layout>
        </Content>
        <Footer style={{textAlign: 'center'}} />
      </Layout>
    )
  }
}

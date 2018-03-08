import React from 'react'
import {BaseLayout} from "../layouts/BaseLayout"


export class Home extends React.Component {
  render() {
    return (
      <BaseLayout children={{
        main: <div>
          <h1>Welcome to the Kubernetes cluster deploy Website!</h1>
        </div>
      }}/>
    )
  }
}


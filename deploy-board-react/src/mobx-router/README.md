# Mobx Router

[Original Idea - How to decouple state and UI](https://medium.com/@mweststrate/how-to-decouple-state-and-ui-a-k-a-you-dont-need-componentwillmount-cc90b787aa37#.k9tvf5nga)

[Implementation partially based on mobx-router](https://github.com/kitze/mobx-router)


## Features
- Decoupled state from UI
- Central route configuration in one file, with multiple views for child layout
- URL changes are triggering changes directly in the store, and vice-versa
- Supported  ```beforeActivate```, ```beforeDeactivate``` route lifecycle async hooks.
- No need to use component lifecycle methods like ```componentWillMount```
  to fetch data or trigger a side effect in the store
- All of the hooks receive
  ```next```, ```router```, ```context```, ```routeChanged```, ```paramsChanged```, ```queryParamsChanged```, ```oldRoute```, ```newRoute```, ```oldParams```, ```newParams```, ```oldQueryParams```, ```newQueryParams```
  as parameters.
  Calling ```next()``` to proceed, otherwise control flow will stop.
- The current URL params and query params are accessible directly in the store ```router.params``` /
  ```router.queryParams``` so basically they're available everywhere without any additional wrapping or HOC.
- Navigating to another view happens by calling the ```go``` method on the router store,
  and the changes in the url are reflected automatically.
  So for example you can call ```router.go({to: "book", params: {id:5, page:3}})``` and
  after the change is made in the router store, the URL change will follow.
  You never directly manipulate the URL or the history object.
- ```<Link to="">``` component which also populates the href attribute and works with middle click or ```cmd/ctrl``` + click

```JSX
import React from 'react';
import ReactDOM from 'react-dom';
import {RouterComponent, RouterStore, Provider} from 'pan-router';


import Home from 'components/Home';
import Document from 'components/Document';
import HomeSidebar from 'components/Sidebar/Home';
import CommonSidebar from 'components/Sidebar/common';
import Login from 'components/Login';
import Logout from 'components/Logout';
import HTTP404Component from 'components/HTTP404Component';

const routes = {
    '/': {
        id: 'home', // you can also name a route by id
        component: <Home/>, // this is the main component
        sidebar: HomeSidebar, // you can have multiple views for each route, for sub layout
        counter: 0,
        beforeActivate({next}) {
            this.counter++;
            console.log(`${this.id} is visited ${this.counter} times`);
            next();
        },
    },
    '/document/:id': {
        id: 'document',
        sidebar: <CommonSidebar/>,
        component: <Document/>,
        beforeActivate({next, router, newRoute, newParams, newQueryParams, context: {appstore: {user}}}) {
            if (!user) {
                let res = window.confirm('Only logged in users can enter. Login now?');
                if (res === true) {
                    // save context so login page can redirect after
                    router.setContext('redirectAfterLogin', {
                        to: newRoute,
                        params: newParams,
                        queryParams: newQueryParams
                    });
                    router.go("/login");
                }
            } else {
                console.log(`Greeting ${user}, welcome back to private section of this site`)
                next()
            }
        }
    },
    '/login': {
        component: <Login/>,
        sidebar: null,
    },
    '/logout': {
        component: <Logout/>,
        sidebar: null,
        beforeActivate(arg) {
            console.log(`logging out user ${arg.router.getContext('mystore').user}`);
            arg.router.getContext('mystore').user = null;
            arg.next();
        }
    },
    '*': {
        id: 'default',
        sidebar: null,
        component: <HTTP404Component/>
    }
};


// your own data store
const mystore = new MyStore();

let router = new RouterStore({
    routes,
    context: {mystore}, // store a context of your data so it can be accessed via router.getContext()
    hashbang: true, // whether to use #! for routes, so that your react app can integrate into existing web projects
    base: '/myapp' // your react app will be integrated into /myapp#!/your_route in your project
});
router.start();

ReactDOM.render(
    <Provider mystore={mystore} router={router}>
        <div>
            <div>
                <RouterComponent group="sidebar"/>
            </div>
            <div>
                <RouterComponent/>
            </div>
        </div>
    </Provider>, document.getElementById('root')
);
```

#### Links to other client side routing libs

    https://github.com/visionmedia/page.js/
    https://github.com/sonaye/pagify-it
    https://github.com/flatiron/director
    https://github.com/mtrpcic/pathjs
    https://github.com/millermedeiros/crossroads.js
    https://github.com/krasimir/navigo/
    https://github.com/ReactTraining/history
    https://github.com/browserstate/history.js
    https://github.com/yoshuawuyts/sheet-router
    https://reactarmory.com/answers
    http://diveintohtml5.info/everything.html


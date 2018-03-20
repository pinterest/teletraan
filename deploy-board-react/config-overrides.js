/* config-overrides.js */
const rewireMobX = require('react-app-rewire-mobx')
const rewireEslint = require('react-app-rewire-eslint')
const Dotenv = require('dotenv-webpack');

module.exports = function override(config, env) {
  // use the MobX rewire
  config = rewireMobX(config, env)
  config = rewireEslint(config, env)
  if (!config.plugins) {
    config.plugins = []
  }
  config.plugins.push(new Dotenv())
  return config
}

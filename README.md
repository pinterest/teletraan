### Teletraan Deploy Service

![](https://github.com/pinterest/teletraan/blob/master/docs/misc/images/TeletraanDefinition.png)

### What is Teletraan?
Teletraan is Pinterest's deploy system. It deploys hundreds of Pinterest internal services, supports tens of thousands hosts, and has been running in production for years. It empowers Pinterest Engineers to deliver their code to pinners fast and safe. Check out the [Design](https://github.com/pinterest/teletraan/wiki/Design) section or our blog post: [Under the hood: Teletraan Deploy System] (https://engineering.pinterest.com/blog/under-hood-teletraan-deploy-system) for more details.

### Why use Teletraan?
Teletraan is designed to do one thing and one thing only - deploy. It supports critical features such as 0 downtime deploy, rollback, staging, continuous deploy; and many convenient features such as showing commit details, comparing different deploys, notifying deploy state changes through either email or chat, displaying openTSDB metrics and more. Teletraan currently does not support container based deploy, it is on our roadmap to support next.

### How to use Teletraan?
Teletraan is designed to be a flexible building block. You can plug Teletraan into your existing release workflow given the following requirements met:
* Run [Deploy Agent](https://github.com/pinterest/teletraan/wiki/Deploy-Agent) and provide [Host Info File](https://github.com/pinterest/teletraan/wiki/Deploy-Agent#host-info-file) and on each host through your provisioning process
* Add [Deploy Scripts](https://github.com/pinterest/teletraan/wiki/Deploy-Agent#deploy-scripts) in your application code
* Publish [Build](https://github.com/pinterest/teletraan/wiki/Introduction) to Teletraan for build artifacts to be deployed

Check out [Integrate with Teletraan](https://github.com/pinterest/teletraan/wiki/Integrate-with-teletraan) for more details.

### Quick start

[Quick start guide!](https://github.com/pinterest/teletraan/wiki/Quickstart-Guide)

### Documentation

[Check out our wiki!](https://github.com/pinterest/teletraan/wiki)

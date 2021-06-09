### Teletraan Deploy Service

![](https://github.com/pinterest/teletraan/blob/master/docs/misc/images/TeletraanIntro.png)

### What is Teletraan?
Teletraan is Pinterest's deploy system. It deploys thousands of Pinterest internal services, supports tens of thousands hosts, and has been running in production for over many years. It empowers Pinterest Engineers to deliver their code to pinners fast and safe. Check out the [wiki](https://github.com/pinterest/teletraan/wiki) or blog post [Under the hood: Teletraan Deploy System](https://medium.com/@Pinterest_Engineering/under-the-hood-teletraan-deploy-system-1e5caa774a63) for more details.

The name Teletraan comes from a character in Transformer TV series! [wikipedia](https://en.wikipedia.org/wiki/List_of_Transformers_supporting_characters#Teletran_3)

### Why use Teletraan?
Teletraan is designed to do one thing and one thing only - deploy. It supports critical features such as 0 downtime deploy, rollback, staging, continuous deploy; and many convenient developer facing features such as showing commit details, comparing different deploys, notifying deploy state changes through email or slack, displaying metrics and more. Teletraan does not support container based deploy yet. Currently you can still use Teletraan Deploy Scripts to call docker or docker-compose to run containers.

### How to use Teletraan?
Teletraan is designed to be a flexible building block. You can plug Teletraan into your existing release workflow given the following requirements met:
* Run Deploy Agent on every host
* Add Deploy Scripts to your application code
* Publish Build Artifacts to Teletraan in the end of each build

Check out [Integrate with Teletraan](https://github.com/pinterest/teletraan/wiki/Integrate-with-teletraan) for more details.

### Quick start

[Quick start guide!](https://github.com/pinterest/teletraan/wiki/Quickstart-Guide)

### Documentation

[Check out our wiki!](https://github.com/pinterest/teletraan/wiki)

### Help

If you have any questions or comments, you can reach us at teletraan-users@googlegroups.com

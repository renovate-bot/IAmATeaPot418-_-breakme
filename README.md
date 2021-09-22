# bRPC

一个基于netty的RPC框架

1：基于netty NIO、IO多路复用。
2：利用zookeeper做服务注册中心。
3：client与server端建立心跳包保活机制。此外client未知断连时，server端主动关闭，触发client channel Inactive事件并进行重连保证长连接。
4：自定义传输包，避免TCP沾包问题。
5：整合spring注解，可通过注解便捷使用，此外可在注解中配置server端业务线程池核心线程数及最大线程数。

## Getting started

To make it easy for you to get started with GitLab, here's a list of recommended next steps.

Already a pro? Just edit this README.md and make it your own. Want to make it easy? [Use the template at the bottom](#editing-this-readme)!






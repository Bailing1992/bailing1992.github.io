---
layout: post
title: "网络 系列 TCP"
subtitle: '开启 网络 探索新篇章'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - network 
---

TCP 是面向连接的（socket连接）、可靠的、基于字节流（bytes）的传输层协议。所谓流，就是指不间断的数据结构，可以把它想象成排水管道中的水流。当应用程序采用TCP发送消息时，虽然可以保证发送的顺序，但还是犹如没有任何间隔的数据流发送给接收端。

字节流服务：两个应用程序通过TCP连接交换 8bit 字节 构成的字节流。TCP不在字节流中插入记录标识符。将这称为字节流服务（bytestreamservice）。TCP 对字节流的内容不作任何解释。TCP不知道传输的数据字节流是二进制数据，还是ASCII字符、EBCDIC 字符或者其他类型数据。对字节流的解释由 TCP 连接双方的应用层解释。

TCP 用于在传输层有必要实现可靠传输的情况，由于它是面向连接并具备顺序控制、重发控制等机制的，所以它可以为应用提供可靠传输。

为了通过 IP 数据报实现可靠性传输，需要考虑很多事情，例如：数据的破坏、丢包、重复以及分片顺序混乱等问题。TCP 通过检验和、序列号、确认应答、重发控制、连接管理以及窗口控制等机制实现可靠性传输。

TCP 作为一种面向有连接的协议，只有在确认通信对端存在时才会发送数据，从而可以控制通信流量的浪费。

MAC 地址和 IP 地址分别用来识别同一链路中不同的计算机以及 TCP/IP 网络中互连的主机和路由器。在传输层，则使用端口号来识别同一台计算机中进行通信的不同应用程序。

![网络](/img/network/21.png)

一般知名端口号在 ```0~1023``` 之间，而经常使用的自定义/动态分配的端口号则一般在```49152~65535``` 之间。

## TCP

TCP 是一种面向连接的单播协议，在发送数据前，通信双方必须在彼此间建立一条连接。所谓的“连接”，其实是客户端和服务器的内存里保存的一份关于对方的信息，如 IP 地址、端口号等。

TCP 可以看成是一种字节流，它会处理 IP 层或以下的层的丢包、重复以及错误问题。在连接的建立过程中，双方需要交换一些连接的参数。这些参数可以放在 TCP 头部。

TCP 提供了一种可靠、面向连接、字节流、传输层的服务，采用三次握手建立一个连接。采用 4 次挥手来关闭一个连接。

![网络](/img/network/22.png)

一个 TCP 连接由一个 4 元组构成，分别是两个 IP 地址和两个端口号。一个 TCP 连接通常分为三个阶段：启动、数据传输、退出（关闭）。

当 TCP 接收到另一端的数据时，它会发送一个确认，但这个确认不会立即发送，一般会延迟一会儿。ACK 是累积的，一个确认字节号 N 的 ACK 表示所有直到 N 的字节（不包括 N）已经成功被接收了。这样的好处是如果一个 ACK 丢失，很可能后续的 ACK 就足以确认前面的报文段了。

一个完整的 TCP 连接是双向和对称的，数据可以在两个方向上平等地流动。给上层应用程序提供一种双工服务。一旦建立了一个连接，这个连接的一个方向上的每个 TCP 报文段都包含了相反方向上的报文段的一个ACK。

序列号的作用是使得一个 TCP 接收端可丢弃重复的报文段，记录以杂乱次序到达的报文段。因为 TCP 使用IP 来传输报文段，而 IP 不提供重复消除或者保证次序正确的功能。另一方面，TCP 是一个字节流协议，绝不会以杂乱的次序给上层程序发送数据。因此 TCP 接收端会被迫先保存大序列号的数据不交给应用程序，直到缺失的小序列号的报文段被填满。

> 为什么建立连接是三次握手，而关闭连接却是四次挥手呢？
这是因为服务端在 LISTEN 状态下，收到建立连接请求的 SYN 报文后，把 ACK 和 SYN 放在一个报文里发送给客户端。而关闭连接时，当收到对方的 FIN 报文时，仅仅表示对方不再发送数据了但是还能接收数据，己方是否现在关闭发送数据通道，需要上层应用来决定，因此，己方ACK和FIN一般都会分开发送。

### 三次握手

三次握手目的是同步连接双方的序列号和确认号并交换TCP窗口大小信息。
![网络](/img/network/10.jpeg)

* 第一次握手： 客户端发送连接请求报文，将 SYN 位置为1，Sequence Number 为 x；然后客户端进入 SYN_SEND 状态，等待服务器的确认
* 第二次握手： 服务器收到客户端的SYN报文段，需要对这个 SYN 报文段进行确认，设置 Acknowledgment Number 为 x+1(Sequence Number+1)。同时，自己还要发送 SYN 请求信息，将 SYN 位置为 1，Sequence Number 为 y。服务器端将上述所有信息放到一个报文段（即 SYN+ACK 报文段）中，一并发送给客户端，此时服务器进入 SYN_RECV 状态；
* 第三次握手： 客户端收到服务器的 SYN+ACK 报文段。然后将 Acknowledgment Number 设置为 y+1，向服务器发送 ACK 报文段，这个报文段发送完毕以后，客户端和服务器端都进入ESTABLISHED 状态，完成 TCP 三次握手。

#### 为什么要三次握手？

**客户端和服务端通信前要进行连接，“3次握手”的作用就是双方都能明确自己和对方的收、发能力是正常的。**

* 第一次握手：客户端发送网络包，服务端收到了。这样服务端就能得出结论：客户端的发送能力、服务端的接收能力是正常的。
* 第二次握手：服务端发包，客户端收到了。这样客户端就能得出结论：服务端的接收、发送能力，客户端的接收、发送能力是正常的。 从客户端的视角来看，接到了服务端发送过来的响应数据包，说明服务端接收到了在第一次握手时发送的网络包，并且成功发送了响应数据包，这就说明，服务端的接收、发送能力正常。而另一方面，收到了服务端的响应数据包，说明第一次发送的网络包成功到达服务端，这样，自己的发送和接收能力也是正常的。
* 第三次握手：客户端发包，服务端收到了。这样服务端就能得出结论：客户端的接收、发送能力，服务端的发送、接收能力是正常的。 第一、二次握手后，服务端并不知道客户端的接收能力以及自己的发送能力是否正常。而在第三次握手时，服务端收到了客户端对第二次握手作的回应。从服务端的角度，在第二次握手时的响应数据发送出去了，客户端接收到了。所以，服务端的发送能力是正常的。而客户端的接收能力也是正常的。

经历了上面的三次握手过程，客户端和服务端都确认了自己的接收、发送能力是正常的。之后就可以正常通信了。

每次都是接收到数据包的一方可以得到一些结论，发送的一方其实没有任何头绪。虽然有发包的动作，但是我怎么知道我有没有发出去，而对方有没有接收到呢？

而从上面的过程可以看到，最少是需要三次握手过程的。两次达不到让双方都得出自己、对方的接收、发送能力都正常的结论。其实每次收到网络包的一方至少是可以得到：对方的发送、我方的接收是正常的。而每一步都是有关联的，下一次的“响应”是由于第一次的“请求”触发，因此每次握手其实是可以得到额外的结论的。比如第三次握手时，服务端收到数据包，表明看服务端只能得到客户端的发送能力、服务端的接收能力是正常的，但是结合第二次，说明服务端在第二次发送的响应包，客户端接收到了，并且作出了响应，从而得到额外的结论：客户端的接收、服务端的发送是正常的。

> 当 Client 端收到 Server 的 SYN+ACK 应答后，其状态变为 ESTABLISHED，并发送 ACK 包给Server。如果此时 ACK 在网络中丢失，那么 Server 端该 TCP 连接的状态为 SYN_RECV，并且依次等待 3 秒、6 秒、12 秒后重新发送 SYN+ACK 包，以便 Client 重新发送 ACK 包。

* Server 重发 SYN+ACK 包的次数，可以通过设置 /proc/sys/net/ipv4/tcp_synack_retries 修改，默认值为 5。
* 如果重发指定次数后，仍然未收到 ACK 应答，那么一段时间后，Server 自动关闭这个连接。
* 但是 Client 认为这个连接已经建立，如果 Client 端向 Server 写数据，Server 端将以 RST 包响应，方能感知到 Server 的错误。

## 四次挥手

数据传输完毕后，双方都可释放连接。最开始的时候，客户端和服务器都是处于ESTABLISHED状态
![网络](/img/network/11.jpeg)

* 进程 1 发出连接释放报文，并且停止发送数据。释放报文首部中 FIN=1、序列号为seq=u（等于前面已经传送过来的数据的最后一个字节的序号加1），此时，进程 1 进入FIN-WAIT-1（终止等待1）状态。 TCP规定，FIN报文段即使不携带数据，也要消耗一个序号。
* 进程 2 收到连接释放报文，发出确认报文，ACK=1，ack=u+1，并且带上自己的序列号seq=v，此时，进程 2 就进入了CLOSE-WAIT（关闭等待）状态。TCP服务器通知高层的应用进程，进程 1 向 进程 2 的方向就释放了，这时候处于半关闭状态，即 进程 1 已经没有数据要发送了，但是 进程 2 若发送数据，客户端依然要接受。这个状态还要持续一段时间，也就是整个CLOSE-WAIT状态持续的时间。
* 进程 1 收到 进程 2 的确认请求后，此时 进程 1 就进入FIN-WAIT-2（终止等待2）状态，等待 进程2 发送连接释放报文（在这之前还需要接受服务器发送的最后的数据）。
* 进程 2 将最后的数据发送完毕后，就向客户端发送连接释放报文，FIN=1，ack=u+1，由于在半关闭状态，进程 2 很可能又发送了一些数据，假定此时的序列号为seq=w，此时，进程 2 就进入了LAST-ACK（最后确认）状态，等待 进程 1 的确认。
* 进程 1 收到 进程 2 的连接释放报文后，必须发出确认，ACK=1，ack=w+1，而自己的序列号是 seq=u+1，此时，进程 1 就进入了 TIME-WAIT（时间等待）状态。注意此时TCP连接还没有释放，必须经过2 * MSL（最长报文段寿命）的时间后，当 进程 1 撤销相应的TCB后，才进入 CLOSED 状态。
* 进程 2 只要收到了 进程 1 发出的确认，立即进入CLOSED状态。同样，撤销TCB后，就结束了这次的TCP连接。可以看到，服务器结束TCP连接的时间要比客户端早一些。

### 为什么要四次分手？

TCP 协议是一种面向连接的、可靠的、基于字节流的运输层通信协议。TCP是全双工模式，这就意味着，当主机1发出FIN报文段时，只是表示主机1已经没有数据要发送了，主机1告诉主机2，它的数据已经全部发送完毕了。但这个时候主机1还是可以接受来自主机2的数据。当主机2返回ACK报文段时，表示它已经知道主机1没有数据发送了，但是主机2还是可以发送数据到主机1的。当主机2也发送了FIN报文段时，这个时候就表示主机2也没有数据要发送了，就会告诉主机1，我也没有数据要发送了，之后彼此就会愉快的中断这次TCP连接。

### 为什么要等待 2MSL？

MSL：报文段最大生存时间，它是任何报文段被丢弃前在网络内的最长时间。原因有二：

* 保证 TCP 协议的全双工连接能够可靠关闭
* 保证这次连接的重复数据段从网络中消失
  
---

1. 第一点：如果主机 1 直接 CLOSED 了，那么由于 IP 协议的不可靠性或者是其它网络原因，导致主机 2 没有收到主机1最后回复的 ACK。那么主机 2 就会在超时之后继续发送 FIN，此时由于主机 1 已经 CLOSED了，就找不到与重发的 FIN 对应的连接。所以，主机1不是直接进入 CLOSED，而是要保持 TIME_WAIT，当再次收到 FIN 的时候，能够保证对方收到 ACK，最后正确的关闭连接。
2. 第二点：如果主机 1 直接 CLOSED，然后又再向主机 2 发起一个新连接，不能保证这个新连接与刚关闭的连接的端口号是不同的。也就是说有可能新连接和老连接的端口号是相同的。一般来说不会发生什么问题，但是还是有特殊情况出现：假设新连接和已经关闭的老连接端口号是一样的，如果前一次连接的某些数据仍然滞留在网络中，这些延迟数据在建立新连接之后才到达主机 2，由于新连接和老连接的端口号是一样的，TCP 协议就认为那个延迟的数据是属于新连接的，这样就和真正的新连接的数据包发生混淆了。所以TCP连接还要在TIME_WAIT 状态等待 2 倍 MSL，这样可以保证本次连接的所有数据都从网络中消失。

## 总结

TCP 通过下列方式来提供可靠性：

1. 应用数据被分割成TCP认为最适合发送的块大小的报文段。这和 UDP 完全不同（应用程序产生的数据报长度将保持不变）
2. 当 TCP 发出一个段后，它启动一个定时器，等待目的端确认收到这个报文段。如果不能及时收到一个确认，将重发这个报文段(超时重发)
3. 当 TCP 收到发自 TCP 连接另一端的数据，它将发送一个确认。这个确认不是立即发送，通常将推迟几分之一秒(之所以推迟，可能是要对包做完整校验)
4. TCP 将保持它首部和数据的检验和。这是一个端到端的检验和，目的是检测数据在传输过程中的任何变化。如果收到段检验和有差错，TCP 将丢弃这个报文段和不确认收到此报文段 (校验出包有错，丢弃报文段，不给出响应，TCP 发送数据端，超时时会重发数据)
5. TCP 报文段作为 IP 数据报来传输，而 IP 数据报的到达可能会失序，因此 TCP 报文段的到达也可能会失序。如果必要，TCP 将对收到的数据进行重新排序，将收到的数据以正确的顺序交给应用层(对失序数据进行重新排序，然后才交给应用层)
6. IP 数据报会发生重复，TCP 的接收端必须丢弃重复的数据(对于重复数据，能够丢弃重复数据)
7. TCP 还能提供流量控制。TCP 连接的每一方都有固定大小的缓冲空间。TCP的接收端只允许另一端发送接收端缓冲区所能接纳的数据。这将防止较快主机致使较慢主机的缓冲区溢出。TCP使用的流量控制协议是可变大小的滑动窗口协议(TCP可以进行流量控制，防止较快主机致使较慢主机的缓冲区溢出)
8. TCP 保证消息顺序：TCP 提供了最可靠的数据传输，它给发送的每个数据包做顺序化（这看起来非常烦琐），然而如果TCP没有这样烦琐的操作，可能会造成更多的麻烦。如造成数据包的重传、顺序的颠倒甚至造成数据包的丢失。

> TCP 具体是通过怎样的方式来保证数据的顺序化传输呢？
主机每次发送数据时，TCP 就给每个数据包分配一个序列号并且在一个特定的时间内等待接收主机对分配的这个序列号进行确认，如果发送主机在一个特定时间内没有收到接收主机的确认，则发送主机会重传此数据包。接收主机利用序列号对接收的数据进行确认，以便检测对方发送的数据是否有丢失或者乱序等，接收主机一旦收到已经顺序化的数据，它就将这些数据按正确的顺序重组成数据流并传递到高层进行处理。具体步骤如下：

1. 为了保证数据包的可靠传递，发送方必须把已发送的数据包保留在缓冲区
2. 为每个已发送的数据包启动一个超时定时器
3. 如在定时器超时之前收到了对方发来的应答信息（可能是对本包的应答，也可以是对本包后续包的应答），则释放该数据包占用的缓冲区
4. 否则重传该数据包，直到收到应答或重传次数超过规定的最大次数为止
5. 接收方收到数据包后，先进行CRC校验，如果正确则把数据交给上层协议，然后给发送方发送一个累计应答包，表明该数据已收到，如果接收方正好也有数据要发给发送方，应答包也可方在数据包中捎带过去

## 参考文献

[“三次握手，四次挥手”你真的懂吗？](https://zhuanlan.zhihu.com/p/53374516)
[跟着动画来学习TCP三次握手和四次挥手](https://juejin.im/post/5b29d2c4e51d4558b80b1d8c)

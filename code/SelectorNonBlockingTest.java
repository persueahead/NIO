package com.spring.demo.test.niotest;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;


/**
 * selector
 * 实现非阻塞
 * 需要配置channel阻塞方式为非阻塞
 * 将channel通过registry以事件的方式注册到selector
 */
public class SelectorNonBlockingTest {
    /**
     * 客户端编写
     * socketChannel.configureBlocking(false);切换通道阻塞方式为非阻塞
     * <p>
     * 结合Scanner(System.in)可以多客户端做聊天室
     *
     * @throws IOException
     */
    @Test
    public void client() throws IOException {
        //获取网络通道
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 9898));
        //切换为非阻塞模式
        socketChannel.configureBlocking(false);
        //分配缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(100);
        //向缓冲区存入数据
        buffer.put(new Date().toString().getBytes());
        buffer.flip();
        //将缓冲区的数据写入通道
        socketChannel.write(buffer);
        buffer.clear();
        //关闭通道
        socketChannel.close();
    }

    /**
     * ServerSocketChannel 面向流的就听套接字的可选通道，用来监听的
     * 为每一个进来的连接都可以创建一个用来读写的socket通道
     * 通过accept()方法
     *
     * @throws IOException
     */
    @Test
    public void server() throws IOException {
        //获取网络通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //配置阻塞方式为非阻塞
        serverSocketChannel.configureBlocking(false);
        //    绑定连接
        serverSocketChannel.bind(new InetSocketAddress(9898));
        //获取选择器
        Selector selector = Selector.open();
        //以事件驱动的方式注册到选择器上 SelectionKey选择键，表示监听的事件 类比accept()阻塞时等待
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        //如果有准备就绪的channel
        while (selector.select() > 0) {
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()) {
                //获取准备就绪的事件
                SelectionKey key = keyIterator.next();
                if (key.isAcceptable()) {
                    //为该链接创建一个socketChannel
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    //    切换为非阻塞是
                    socketChannel.configureBlocking(false);
                    //    将该通道也注册到选择器上  监听它的读事件
                    socketChannel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {//如果就是得失读事件
                    //获取读就绪的channel
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    //    创建缓冲区，来读
                    ByteBuffer buffer = ByteBuffer.allocate(100);
                    int len = 0;
                    while ((len = socketChannel.read(buffer)) > 0) {
                        buffer.flip();
                        System.out.println(new String(buffer.array(), 0, buffer.limit()));
                        buffer.clear();
                    }
                }
                //取消选择键 避免一直是就绪的状态
                keyIterator.remove();
            }
        }
    }


//    --------------UDP类型通道

    /**
     * 发送端
     *
     * @throws IOException
     */
    @Test
    public void send() throws IOException {
        //获取通道  使用UDP(面向数据包的sockets)的channel
        DatagramChannel datagramChannel = DatagramChannel.open();
        //配置非阻塞
        datagramChannel.configureBlocking(false);
        //分配缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(100);
        //扫描器
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            //获取输入的值
            String str = scanner.next();
            buffer.put((new Date().toString() + ":\n" + str).getBytes());
            buffer.flip();
            //将缓冲区的数据发送到服务端
            datagramChannel.send(buffer, new InetSocketAddress("127.0.0.1", 9898));
            buffer.clear();
        }
        datagramChannel.close();
    }

    @Test
    public void receive() throws IOException {
        //获取通道
        DatagramChannel datagramChannel = DatagramChannel.open();
        //配置为非阻塞方式
        datagramChannel.configureBlocking(false);
        //绑定连接
        datagramChannel.bind(new InetSocketAddress(9898));
        //创建selector
        Selector selector = Selector.open();
        //注册读事件到selector
        datagramChannel.register(selector, SelectionKey.OP_READ);
        /*
        选择一组键，其对应的通道已准备好进行I/O操作。
        这个方法执行一个阻塞选择操作。它只在至少一个通道被选择，这个选择器的唤醒方法被调用，或者当前线程被中断后才返回，无论哪个先出现。
         */
        while (selector.select() > 0) {
            //iterator:返回集合元素的迭代器
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                //如果是读就绪的channel
                if (key.isReadable()) {
                    ByteBuffer buffer = ByteBuffer.allocate(100);
                    //将从客户端接受到的数据包传到buffer中 装不下的话，剩余的数据会被丢弃
                    datagramChannel.receive(buffer);
                    buffer.flip();
                    System.out.println(new String(buffer.array(), 0, buffer.limit()));
                    buffer.clear();
                }
            }
            //从基础集合中移除该迭代器返回的最后一个元素。
            keyIterator.remove();
        }


    }
}


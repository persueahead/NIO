package com.spring.demo.test.niotest;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 *
 */
public class SocketChannelDefaultBlockingTest {
    /**
     * 客户端编写
     * @throws IOException
     */
    @Test
    public void client() throws IOException {
        //获取网络通道
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 9898));
        // 分配指定缓冲区大小
        ByteBuffer buffer = ByteBuffer.allocate(300);
        //获取本地文件通道
        FileChannel fileChannel = FileChannel.open(Paths.get("D:\\IDEAworkplace\\spring-demo\\src\\main\\resources\\1.txt"), StandardOpenOption.READ);
        //    读取本地文件到缓冲区，并发送到服务端   read: 读取的字节数，可能为零，如果通道已到达流结束，则为-1
        while (fileChannel.read(buffer) != -1) {
            buffer.flip();
            System.out.println(new String(buffer.array(), 0, buffer.limit()));
            socketChannel.write(buffer);
            buffer.clear();
        }
        //结束输出到通道，告诉服务端，发送数据完毕，不然下面的read会阻塞
        socketChannel.shutdownOutput();
        //接收服务端的反馈
        int len = 0;
        //read方法阻塞了
        while ((len = socketChannel.read(buffer)) != -1){
            buffer.flip();
            System.out.println("反馈"+new String(buffer.array(),0,buffer.limit()));
            buffer.clear();
        }
            //    关闭通道
        socketChannel.close();
        fileChannel.close();
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
        //    绑定连接
        serverSocketChannel.bind(new InetSocketAddress(9898));
        //    获取客户端连接的通道
        SocketChannel socketchannel = serverSocketChannel.accept();
        //本地文件通道
        FileChannel outChannel = FileChannel.open(Paths.get("D:\\IDEAworkplace\\spring-demo\\src\\main\\resources\\3.txt"), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        //分配指定缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(300);
        //接收客户端数据，并保存到本地  read:从通道读到缓冲区
        while (socketchannel.read(buffer) != -1) {
            buffer.flip();
            outChannel.write(buffer);//write：从缓冲区写到通道
            buffer.clear();
        }
        //服务端发送反馈给客户端
        buffer.put("你好，服务端接收数据成功".getBytes());
        buffer.flip();
        socketchannel.write(buffer);//从缓冲区写到通道
        //结束读入通道操作，告诉客户端，读入完毕
        socketchannel.shutdownInput();
        System.out.println("服务端反馈完毕");

        //关闭通道
        outChannel.close();
        socketchannel.close();
        serverSocketChannel.close();


    }
}

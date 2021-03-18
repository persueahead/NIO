package com.spring.demo.test.niotest;

import org.junit.Test;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ScatterAndGatherTest {
    /**
     * 分散与聚集
     * RandomAccessFile 也只能从文件系统中读取，不能从类路径下读取
     * 分散是将字节分散到多个缓冲区，但是如果一个能放下，就没有可分的
     */
    @Test
    public void test() {
        //随机访问文件流
        RandomAccessFile randomAccessFile1 = null;
        //A channel for reading, writing, mapping, and manipulating a file.
        FileChannel channel1 = null;
        RandomAccessFile randomAccessFile2 = null;
        FileChannel channel2 = null;
        try {
            randomAccessFile1 = new RandomAccessFile("D:\\IDEAworkplace\\spring-demo\\src\\main\\resources\\1.txt", "rw");
            //获取通道
            channel1 = randomAccessFile1.getChannel();
            //缓冲区1
            ByteBuffer buffer1 = ByteBuffer.allocate(5);
            //缓冲区2
            ByteBuffer buffer2 = ByteBuffer.allocate(10);

            //分散读取--------------------------
            //初始化buffer数组
            ByteBuffer[] buffers = {buffer1, buffer2};
            //分散读：将该通道的字节序列 分散 读到多个缓冲区 合起来才是完整的字节序列
            channel1.read(buffers,0,buffers.length);
            //channel1.read(buffer1);
            //channel1.read(buffer2);

            for (ByteBuffer byteBuffer : buffers) {
                //设置为读取状态
                byteBuffer.flip();
            }
            //aaaab
            System.out.println(new String(buffers[0].array(),0,buffers[0].limit()));
            System.out.println("---------------------");
            //bbbcccc
            System.out.println(new String(buffers[1].array(),0,buffers[1].limit()));

            //聚集写
            randomAccessFile2 = new RandomAccessFile("D:\\IDEAworkplace\\spring-demo\\src\\main\\resources\\2.txt","rw");
            channel2 = randomAccessFile2.getChannel();
            //aaaabbbbcccc
            channel2.write(buffers);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

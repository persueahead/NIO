package com.spring.demo.test.niotest;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @note Paths 是从文件系统下查找
 */
public class ChannelTest {
    /**
     * 利用通道完成文件的复制（非直接缓冲区）
     * @throws IOException
     */
    @Test
    public void test1() throws IOException {
        FileInputStream fileInputStream = new FileInputStream("D:\\IDEAworkplace\\spring-demo\\src\\main\\resources\\static\\1.jpg");
        FileOutputStream fileOutputStream = new FileOutputStream("D:\\IDEAworkplace\\spring-demo\\src\\main\\resources\\static\\2.jpg");

        //获取通道
        FileChannel inChannel = fileInputStream.getChannel();
        FileChannel outChannel = fileOutputStream.getChannel();

        //分配指定大小的缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        //将in通道中的数据存入缓冲区
        while (inChannel.read(buffer) != -1){
            //切换到读取状态
            buffer.flip();
            //将缓冲区数据写入out通道
            outChannel.write(buffer);
            buffer.clear();
        }
        outChannel.close();
        inChannel.close();
        fileOutputStream.close();
        fileInputStream.close();
    }

    /**
     * 使用注解缓冲区完成文件的复制 （内存映射文件的方式）
     * @throws IOException
     */
    @Test
    public void channelUseDirectTest() throws IOException {
        FileChannel inChannel = FileChannel.open(Paths.get("D:\\IDEAworkplace\\spring-demo\\src\\main\\resources\\static\\1.jpg"), StandardOpenOption.READ);
        FileChannel outChannel = FileChannel.open(Paths.get("D:\\IDEAworkplace\\spring-demo\\src\\main\\resources\\static\\2.jpg"),StandardOpenOption.WRITE,StandardOpenOption.READ,StandardOpenOption.CREATE_NEW);

        //内存映射文件
        MappedByteBuffer inMappedBuf = inChannel.map(FileChannel.MapMode.READ_ONLY,0,inChannel.size());
        MappedByteBuffer outMappedBuf = outChannel.map(FileChannel.MapMode.READ_WRITE,0,inChannel.size());

        //直接对缓冲区进行数据的读写操作
        byte[] dst = new byte[inMappedBuf.limit()];
        //传输数据到给定的数组
        inMappedBuf.get(dst);
        //将给定数组的完整数据传到这个buffer
        outMappedBuf.put(dst);

        inChannel.close();
        outChannel.close();
    }

    /**
     * 直接用通道间的传输
     * 也是用的直接缓冲区
     */
    @Test
    public void transformDirChannel() throws IOException {
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            //这里使用的Maven，但是Paths不搜索类路径中的文件,而是搜索文件系统中的文件. 这里取不到文件
            //inChannel = FileChannel.open(Paths.get("static/1.jpg"), StandardOpenOption.READ);
            inChannel = FileChannel.open(Paths.get("D:\\IDEAworkplace\\spring-demo\\src\\main\\resources\\static\\1.jpg"), StandardOpenOption.READ);
            outChannel = FileChannel.open(Paths.get("D:\\IDEAworkplace\\spring-demo\\src\\main\\resources\\static\\2.jpg"), StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE_NEW);
            //到哪去
            inChannel.transferTo(0, inChannel.size(), outChannel);
            //从哪来
            //outChannel.transferFrom(inChannel,0,inChannel.size());
            outChannel.close();
            inChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

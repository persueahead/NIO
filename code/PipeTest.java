package com.spring.demo.test.niotest;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

public class PipeTest {
    @Test
    public void pipeTest() throws IOException {
        //获取管道
        Pipe pipe = Pipe.open();
        //将缓冲区的数据写入管道
        ByteBuffer buffer = ByteBuffer.allocate(100);
        Pipe.SinkChannel sink = pipe.sink();//表示管道可写端的通道
        buffer.put("通过单向管道发送数据".getBytes());
        buffer.flip();
        sink.write(buffer);//利用sink将buffer中的数据写入管道

        //上面的可以放到一个线程中，下面的又可以放到一个线程中，进行单向传输----------------------

        //    读取管道中的数据
        Pipe.SourceChannel source = pipe.source();//获取管道的可读端
        buffer.flip();
        int len = source.read(buffer);//将该channel的字节读到buffer中,len为读取的字节数
        System.out.println(new String(buffer.array(),0,len));
    }
}

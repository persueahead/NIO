package com.spring.demo.test.niotest;

import org.junit.Test;

import java.nio.ByteBuffer;

public class BufferTest {
    /**
     * 创建buffer
     */
    @Test
    public void testCreate(){
        //创建并制定缓冲区大小
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        System.out.println("buffer信息");
        System.out.println(byteBuffer.mark());//返回的是重置position后的当前buffer对象
        System.out.println(byteBuffer == byteBuffer.mark());//true
        System.out.println(byteBuffer.position());//0
        System.out.println(byteBuffer.limit());//1024
        System.out.println(byteBuffer.capacity());//1024
    }

    /**
     * 测试
     * 这里是非直接缓冲区
     */
    @Test
    public void putAndReadTest(){
        System.out.println("create");
        ByteBuffer byteBuffer = ByteBuffer.allocate(10);
        System.out.println(byteBuffer.position());//0
        System.out.println(byteBuffer.limit());//10
        System.out.println(byteBuffer.capacity());//10

        System.out.println("put----");
        byteBuffer.put("abcde".getBytes());//将数据存到缓冲区
        System.out.println(byteBuffer.position());//5
        System.out.println(byteBuffer.limit());//10
        System.out.println(byteBuffer.capacity());//10

        System.out.println("read准备--");
        byteBuffer.flip();//为get做准：limit移到position的位置，position移到0，mark重置为-1
        System.out.println(byteBuffer.position());//0
        System.out.println(byteBuffer.limit());//5
        System.out.println(byteBuffer.capacity());//10

        System.out.println("read--");
        byte[] data = new byte[byteBuffer.limit()];
        byteBuffer.get(data);//将缓冲区内容转移到指定数组中
        System.out.println(new String(data,0,data.length));//abcde
        System.out.println(byteBuffer.position());//5
        System.out.println(byteBuffer.limit());//5
        System.out.println(byteBuffer.capacity());//10

        //System.out.println("读完一遍，直接重读，报错，position到limit间冒的了");
        //byte[] newData = new byte[byteBuffer.limit()];
        //byteBuffer.get(newData);//java.nio.BufferUnderflowException缓冲区下溢异常，
        //System.out.println(new String(newData,0,newData.length));

        System.out.println("准备重读：rewind--");
        byteBuffer.rewind();//又可以正常读数据了
        byte[] arr = new byte[2];
        byteBuffer.get(arr,0,2);
        System.out.println(new String(arr,0,arr.length));//ab
        System.out.println(byteBuffer.position());//2
        System.out.println(byteBuffer.limit());//5
        System.out.println(byteBuffer.capacity());//10
        System.out.println("查看剩余position到limit的数据个数--");
        System.out.println(byteBuffer.remaining());//3
        System.out.println("标记mark");
        byteBuffer.mark();//标记mark为当前position位置 2
        int count = byteBuffer.remaining();
        for(int i=0;i<count;i++){
            System.out.println(byteBuffer.get());
        }
        System.out.println("读取后的position："+byteBuffer.position());//5
        System.out.println("reset position为mark标记的位置");
        byteBuffer.reset();
        System.out.println("利用mark重置后的position："+byteBuffer.position() );//2


        System.out.println("clear，恢复到初始状态--");
        byteBuffer.clear();//此时limit不是元素长度，而是缓冲区容量（capacity），不能正常读取数据，mark重置为-1
        System.out.println(byteBuffer.position());//0
        System.out.println(byteBuffer.limit());//10
        System.out.println(byteBuffer.capacity());//10
    }

    /**
     * 直接缓冲区测试
     */
    @Test
    public void directBufferTest(){
        //分配直接缓冲区
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(10);
        //true
        System.out.println(byteBuffer.isDirect());
        //java.nio.DirectByteBuffer[pos=0 lim=10 cap=10]
        System.out.println(byteBuffer);
        byteBuffer = null;
    }
}

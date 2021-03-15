# 介绍

 Java NIO（New IO）是从**Java 1.4**版本开始引入的一个新的**IO** API，可以替代标准的Java IO API。NIO与原来的IO有同样的作用和目的，但是使用的方式完全不同，**NIO支持面向缓冲区的、基于通道的IO操作。NIO将以更加高效的方式进行文件的读写操作。**

# 预备知识

## 内核态(内核空间)和用户态(用户空间)的区别和联系

https://www.cnblogs.com/jswang/p/9049229.html

https://juejin.cn/post/6916564528587243527

用户空间就是用户进程所在的内存区域，相对的，系统空间就是操作系统占据的内存区域。用户进程和系统进程的所有数据都在内存中。

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3dfd790a6b6448668c23c56d5afd6ba0~tplv-k3u1fbpfcp-zoom-1.image)

用户态的程序不能随意操作内核地址空间，这样对操作系统具有一定的安全保护作用。

linux整体结构：

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a149aa63f28b40739cce0207d3275446~tplv-k3u1fbpfcp-zoom-1.image)

**当一个任务（进程）执行系统调用而陷入内核代码中执行时，称进程处于内核运行态（内核态）**

## 直接内存

参考： https://blog.csdn.net/qq_33521184/article/details/105622931

### jvm体系结构

![image-20210315003816351](C:\Users\夜神\AppData\Roaming\Typora\typora-user-images\image-20210315003816351.png)

![0 5 和  javafi, 现 在 叫 虚 似  class 对 象 存 储 在 氵 去  区 中  在 内 存 中 ， 多 线 程 共  亨 堆 和 方 法 区  机 行 引 擎 ： 翻 译 自 ，  将 高 级 自 翻 译 成 机  入 口  Wt 》 a  执 行 引  0 （ ut 处 0  Engine  Cla ” loader  (RuntimeData Area  程 序 计 獭 器  Program （ 0 怡 r Register  将 jvm 字 节 码 文 件  载 到 内 存 中 。 生 成  Class 对 象  栈 ， 程 序 畜 存 器 是 衝  程 序 计 数 器 。 也 叫 PC  本 地 方 口 ： ' | 本 地 方 法 庳 ](file:///C:/Users/夜神/AppData/Local/Temp/msohtmlclip1/01/clip_image001.png)

直接内存并不属于JVM的内存结构，它是物理机的内存，但是JVM虚拟机可以调用该部分内存。

### 直接内存的使用

- 常见于`NIO`，用于数据缓冲区
- 分配回收的代价较高，但是速度很快
- 不受`JVM`内存回收管理

### 正常IO读取

![正常IO读取结构图](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9jZG4uanNkZWxpdnIubmV0L2doL2NyYXp5anVtcy9jcmF6eWp1bXMuZ2l0aHViLmlvQG1hc3Rlci9pbWFnZXMvYXJ0aWNsZS8yMDIwMDQxNTEzNTczMy5wbmc?x-oss-process=image/format,png)

当`java`程序需要读取文件时，首先会在java堆内存中`new`一个缓冲区，然后系统内存从磁盘中读取文件，再然后在将系统缓冲区中的字节流复制到java堆内存的缓冲区中，然后在由java程序调用。

效率低，多余的copy，开辟两个缓冲区（内存块），双方互相copy

### 直接IO读取

![直接内存结构图](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9jZG4uanNkZWxpdnIubmV0L2doL2NyYXp5anVtcy9jcmF6eWp1bXMuZ2l0aHViLmlvQG1hc3Rlci9pbWFnZXMvYXJ0aWNsZS8yMDIwMDQxNTEzNTkxOS5wbmc?x-oss-process=image/format,png)

当java程序使用直接内存时，首先java程序在系统内存中分配一块直接内存块，这一内存块是系统内存和java堆内存可以**共享**的，那么系统内存读取到的磁盘文件就可以直接由java堆内存使用，这样就省去了复制的操作，大大节约了时间开销。

### 直接内存分配

通过`java`中的**`unsafe`**对象分配一块直接内存，直接内存大小在分配时指定。**直接内存由于不受`JVM`的管理**，所以直接内存的释放，必须主动调用`unsafe`对象进行**释放**，才能将直接内存释放。

```java
DirectByteBuffer(int cap) {                   // package-private

        super(-1, 0, cap, cap);
        boolean pa = VM.isDirectMemoryPageAligned();
        int ps = Bits.pageSize();
        long size = Math.max(1L, (long)cap + (pa ? ps : 0));
        Bits.reserveMemory(size, cap);

        long base = 0;
        try {
            base = unsafe.allocateMemory(size); //分配直接内存，并返回基地址
        } catch (OutOfMemoryError x) {
            Bits.unreserveMemory(size, cap);
            throw x;
        }
        unsafe.setMemory(base, size, (byte) 0);//内存初始化
        if (pa && (base % ps != 0)) {
            // Round up to page boundary
            address = base + ps - (base & (ps - 1));
        } else {
            address = base;
        }
    	/*
    	*跟踪DirectByteBuffer对象的垃圾回收，以实现堆外内存释放
    	*cleaner对象用来释放直接内存，cleaner对象关联了当前的ByteBuffer对象，因为ByteBuffer对象是受java虚拟机管理的，直接内存不受java虚拟机管理，所以这里的关联，就是为了在当ByteBuffer被释放的时候，直接内存也被释放，只不过是被unsafe对象释放的，并不是Java虚拟机释放的。
    	*/
        cleaner = Cleaner.create(this, new Deallocator(base, size, cap));//释放直接内存
        att = null;
    }
```

解释：这里使用**cleaner对象**用来释放直接内存，cleaner对象关联了当前的ByteBuffer对象，因为ByteBuffer对象是受java虚拟机管理的，直接内存不受java虚拟机管理，所以这里的关联，就是**为了在当ByteBuffer被释放的时候，直接内存也被释放，只不过是被unsafe对象释放的，并不是Java虚拟机释放的**。

cleaner对象是一个**虚引用对象**。

Deallocator

```java
private static class Deallocator
        implements Runnable
    {

        private static Unsafe unsafe = Unsafe.getUnsafe();

        private long address;
        private long size;
        private int capacity;

        private Deallocator(long address, long size, int capacity) {
            assert (address != 0);
            this.address = address;
            this.size = size;
            this.capacity = capacity;
        }

        public void run() {
            if (address == 0) {
                // Paranoia
                return;
            }
            unsafe.freeMemory(address);//主动释放直接内存
            address = 0;
            Bits.unreserveMemory(size, capacity);
        }

    }
```

### unsafe类

参考： http://blog.itpub.net/31559353/viewspace-2636126/

#### 介绍

Unsafe是位于sun.misc包下的一个类，主要提供一些用于执行低级别、不安全操作的方法，如直接访问系统内存资源、自主管理内存资源等，这些方法在提升Java运行效率、增强Java语言底层资源操作能力方面起到了很大的作用。但由于Unsafe类使Java语言拥有了类似C语言指针一样操作内存空间的能力，这无疑也增加了程序发生相关指针问题的风险。在程序中过度、不正确使用Unsafe类会使得程序出错的概率变大，使得Java这种安全的语言变得不再“安全”，因此对Unsafe的使用一定要慎重。

```java
package sun.misc;
。。。 。。。

public final class Unsafe {
     // 单例对象
    private static final Unsafe theUnsafe;
    public static final int INVALID_FIELD_OFFSET = -1;
	。。。 。。。

    private static native void registerNatives();

    private Unsafe() {
    }

    @CallerSensitive
    public static Unsafe getUnsafe() {
        Class var0 = Reflection.getCallerClass();
         // 仅在引导类加载器`BootstrapClassLoader`加载时才合法
        if (!VM.isSystemDomainLoader(var0.getClassLoader())) {
            throw new SecurityException("Unsafe");
        } else {
            return theUnsafe;
        }
    }

    public native int getInt(Object var1, long var2);
。。。 。。。
```

![img](http://img.blog.itpub.net/blog/2019/02/15/4d4f5d4616733031.jpeg?x-oss-process=style/bb)

#### 内存操作

```java
//分配内存, 相当于C++的malloc函数
public native long allocateMemory(long bytes);
//扩充内存
public native long reallocateMemory(long address, long bytes);
//释放内存
public native void freeMemory(long address);
//在给定的内存块中设置值
public native void setMemory(Object o, long offset, long bytes, byte value);
//内存拷贝
public native void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes);
//获取给定地址值，忽略修饰限定符的限制访问限制。与此类似操作还有: getInt，getDouble，getLong，getChar等
public native Object getObject(Object o, long offset);
//为给定地址设置值，忽略修饰限定符的访问限制，与此类似操作还有: putInt,putDouble，putLong，putChar等
public native void putObject(Object o, long offset, Object x);
//获取给定地址的byte类型的值(当且仅当该内存地址为allocateMemory分配时，此方法结果为确定的)
public native byte getByte(long address);
//为给定地址设置byte类型的值(当且仅当该内存地址为allocateMemory分配时，此方法结果才是确定的)
public native void putByte(long address, byte x);
```

通常，我们在Java中创建的对象都处于堆内内存（heap）中，堆内内存是由JVM所管控的Java进程内存，并且它们遵循JVM的内存管理机制，JVM会采用垃圾回收机制统一管理堆内存。与之相对的是堆外内存，存在于JVM管控之外的内存区域，Java中对堆外内存的操作，依赖于Unsafe提供的操作堆外内存的native方法。

### 直接内存回收

直接内存的会随着ByteBuffer对象的被回收，然后触发cleaner对象，调用Unsafe对象将直接内存回收，看起来也像是一种自动回收的方法。

但是，由于ByteBuffer对象的回收，是遵循**JVM回收机制**的，也就是说，得达到一定的回收条件才会回收ByteBuffer对象。那么直接内存也不会被回收，这样就会导致内存不足。所以建议使用手动调用Unsafe的方法释放直接内存。

## 操作系统空间和jvm空间的区别与联系

参考：  http://www.360doc.com/content/20/0905/17/835902_934130132.shtml

1. **操作系统分为栈和堆**，栈由操作系统管理，会有操作系统进行自动回收，堆由用户进行分配使用
2. JVM内存使用的操作系统的堆，以防JVM分配的内存被操作系统回收
3. **JVM本地方法栈指的是操作系统的栈**
4. 操作系统的PC寄存器，是计算机上的存储硬件，与内存条一样的硬件，但是寄存区位于CPU内，被称为Cache，用于加快数据访问速度。内存是外挂在CPU的数据总线上的
5. **JVM PC寄存器位于操作系统的堆中**



参考：，jvm内存需要增加一个本地方法栈

![image-20210315003641234](C:\Users\夜神\AppData\Roaming\Typora\typora-user-images\image-20210315003641234.png)

# NIO与IO的主要区别

| IO                      | NIO                         |
| ----------------------- | --------------------------- |
| 面向流(Stream Oriented) | 面向缓冲区(Buffer Oriented) |
| 阻塞IO(Blocking IO)     | 非阻塞IO(Non Blocking IO)   |
| (无)                    | 选择器(Selectors)           |

## 面向

IO流，直接就是数据以流的形式传输，流有起点，终点，不可逆

![image-20210313214535875](C:\Users\夜神\AppData\Roaming\Typora\typora-user-images\image-20210313214535875.png)

NIO 输入和输出都是通过缓冲区，缓冲区是双向的

![image-20210313221325995](C:\Users\夜神\AppData\Roaming\Typora\typora-user-images\image-20210313221325995.png)

# 通道（Channel ）与缓冲区（Buffer）

Java NIO系统的核心在于:通道(Channel)和缓冲区(Buffer)。通道表示打开到10设备(例如:文件、套接字)的连接。若需要使用NIO系统，需要获取用于连接I0设备的通道以及用于容纳数据的缓冲区。然后操作缓冲区，对数据进行处理。

**简而言之，Channel 负责传输，Buffer 负责存储**

## 缓冲区

### 举例  ByteBuffer

```java
public abstract class ByteBuffer
    extends Buffer
    implements Comparable<ByteBuffer>
{
    // These fields are declared here rather than in Heap-X-Buffer in order to
    // reduce the number of virtual method invocations needed to access these
    // values, which is especially costly when coding small buffers.
    //
    final byte[] hb;                  // Non-null only for heap buffers
    final int offset;
    boolean isReadOnly;                 // Valid only for heap buffers
    ... ... 
}
```

### Buffer接口

```java
/**
 *存放特定基本内容的容器
 *A container for data of a specific primitive type.
 * 是一个指定的基本类型元素的序列
 * A buffer is a linear, finite sequence of elements of a specific
 * primitive type.  
 * 必不可少的属性是它的容量，限制，位置
 * Aside from its content, the essential properties of a
 * buffer are its capacity, limit, and position:
*/
public abstract class Buffer {

    /**
     * The characteristics of Spliterators that traverse and split elements
     * maintained in Buffers.
     */
    static final int SPLITERATOR_CHARACTERISTICS =
        Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.ORDERED;

    // Invariants: mark <= position <= limit <= capacity
    private int mark = -1;
    private int position = 0;
    private int limit;
    private int capacity;

    // Used only by direct buffers
    // NOTE: hoisted here for speed in JNI GetDirectBufferAddress
    long address;
...
}
```

### 四个核心属性

JDK8里面，有五个属性，四个是核心属性

```java
    // Invariants: mark <= position <= limit <= capacity
    private int mark = -1;
    private int position = 0;
    private int limit;
    private int capacity;


    // Used only by direct buffers
    // NOTE: hoisted here for speed in JNI GetDirectBufferAddress
    long address;
```
 **容量** (capacity)  ：表示 Buffer 最大数据容量，缓冲区容量不能为负，并且创建后不能更改。
        **限制** (limit) ：第一个不应该读取或写入的数据的索引，即位于 limit 后的数据不可读写。缓冲区的限制不能为负，并且不能大于其容量。
        **位置** (position)： ：下一个要读取或写入的数据的索引。缓冲区的位置不能为负，并且不能大于其限制
        **标记** (mark) 与**重置** (reset) ：标记是一个索引，通过 Buffer 中的 mark() 方法指定 Buffer 中一个特定的 position，之后可以通过调用 **reset()** 方法恢复到这个 position.  

不变性：标记 、 位置 、 限制 、 容量遵守以下不变式：

  mark <= position <= limit <= capacity

mark只是默认为-1，除开默认情况， 0< mark <= position <= limit <= capacity

![image-20210314170621605](C:\Users\夜神\AppData\Roaming\Typora\typora-user-images\image-20210314170621605.png)

### 核心方法

![image-20210314170831344](C:\Users\夜神\AppData\Roaming\Typora\typora-user-images\image-20210314170831344.png)

#### allocate

初始化一个给定容量的 **非直接** buffer(缓冲区)

mark：-1
       position：0
       limit：capacity
       capacity: 指定



```java
    public static ByteBuffer allocate(int capacity) {
        if (capacity < 0)
            throw new IllegalArgumentException();
        return new HeapByteBuffer(capacity, capacity);
    }

    HeapByteBuffer(int cap, int lim) {            // package-private
        super(-1, 0, lim, cap, new byte[cap], 0);
        /*
        hb = new byte[cap];
        offset = 0;
        */
    }
    ByteBuffer(int mark, int pos, int lim, int cap,   // package-private
                 byte[] hb, int offset)
    {
        super(mark, pos, lim, cap);
        this.hb = hb;
        this.offset = offset;
    }
```

#### allocateDirect

初始化一个**直接** 缓冲区

```java
    public static ByteBuffer allocateDirect(int capacity) {
        return new DirectByteBuffer(capacity);
    }

 // Primary constructor
    //
    DirectByteBuffer(int cap) {                   // package-private

        super(-1, 0, cap, cap);
        boolean pa = VM.isDirectMemoryPageAligned();
        int ps = Bits.pageSize();
        long size = Math.max(1L, (long)cap + (pa ? ps : 0));
        Bits.reserveMemory(size, cap);

        long base = 0;
        try {
            base = unsafe.allocateMemory(size);
        } catch (OutOfMemoryError x) {
            Bits.unreserveMemory(size, cap);
            throw x;
        }
        unsafe.setMemory(base, size, (byte) 0);
        if (pa && (base % ps != 0)) {
            // Round up to page boundary
            address = base + ps - (base & (ps - 1));
        } else {
            address = base;
        }
        cleaner = Cleaner.create(this, new Deallocator(base, size, cap));
        att = null;
    }

    MappedByteBuffer(int mark, int pos, int lim, int cap) { // package-private
        super(mark, pos, lim, cap);
        this.fd = null;
    }

    // Creates a new buffer with the given mark, position, limit, and capacity
    //
    ByteBuffer(int mark, int pos, int lim, int cap) { // package-private
        this(mark, pos, lim, cap, null, 0);
    }
```



#### put

将给定的数据全部存放到缓冲区

```java
    public final ByteBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }
```

可以指定范围，不能大于缓冲区position到limit的容量

```
    public ByteBuffer put(byte[] src, int offset, int length) {
        checkBounds(offset, length, src.length);
        if (length > remaining())
            throw new BufferOverflowException();
        int end = offset + length;
        for (int i = offset; i < end; i++)
            this.put(src[i]);
        return this;
    }
```

#### flip

翻转，为读取数据做准备,注意，重置了mark为-1

```java
    public final Buffer flip() {
        limit = position;
        position = 0;
        mark = -1;
        return this;
    }
```



#### get

读，从position位置开始读，读到limit （左闭右开），读一个，position+1

get()读当前位置

```java
    public abstract byte get();
    
    public byte get() {
        return hb[ix(nextGetIndex())];
    }
    protected int ix(int i) {
        return i + offset;
    }
    final int nextGetIndex() {                          // package-private
        if (position >= limit) //position>=limit了还来读就该报错了
            throw new BufferUnderflowException();
        //返回position，之后，再position+1
        return position++;
    }
```

get() 将缓冲区内容读到指定数组，数组长度不能大于缓冲区元素个数

```java
    public ByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }
```

get(byte[] dst, int offset, int length)  指定读多少数据到给定数组

```java
    public ByteBuffer get(byte[] dst, int offset, int length) {
        //检查指定的值是否造成下标越界
        checkBounds(offset, length, dst.length);
        if (length > remaining())
            //如果数组长度大于position到limit间元素个数，抛出buffer下溢异常
            throw new BufferUnderflowException();
        int end = offset + length;
        for (int i = offset; i < end; i++)
            //从当前psition开始，读（end-offset）次
            dst[i] = get();
        return this;
    }
```



#### rewind

准备重读

注意，**重置mark为-1了，且没有更改limit**，so只能是在读环境下进行重读，此时limit是实际元素长度而不是数组容量

```java
    public final Buffer rewind() {
        position = 0;
        mark = -1;
        return this;
    }
```



#### remaining

查看position到limit间元素个数

```java
    public final int remaining() {
        return limit - position;
    }
```

#### mark

记录此时position的位置

```java
    public final Buffer mark() {
        mark = position;
        return this;
    }
```

#### reset

重置position为mark记录的值,显然mark要>0

```java
    public final Buffer reset() {
        int m = mark;
        if (m < 0)
            throw new InvalidMarkException();
        position = m;
        return this;
    }
```

#### clear

返回个各个属性的初始化buffer时的位置

注意是清除对各属性的改动，不是清空缓存

```java
    public final Buffer clear() {
        position = 0;
        limit = capacity;
        mark = -1;
        return this;
    }
```

#### 测试

```java
  /**
     * 测试
     *@note 这里的都是非直接缓冲区
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
```







### 直接缓冲区和非直接缓冲区

#### 非直接缓冲区

用户空间就是用户进程所在的内存区域，相对的，系统空间就是操作系统占据的内存区域。用户进程和系统进程的所有数据都在内存中。

![img](https://images2017.cnblogs.com/blog/307536/201707/307536-20170731145300974-520326124.png)

#### 直接缓冲区

![img](https://images2017.cnblogs.com/blog/307536/201707/307536-20170731145311224-406164516.png)

#### 比较

非直接缓冲区：通过 allocate() 方法分配缓冲区，**将缓冲区建立在 JVM 的内存中**

直接缓冲区：通过 allocateDirect() 方法分配直接缓冲区，**将缓冲区建立在物理内存中**。可以提高效率

- 字节缓冲区要么是直接的，要么是非直接的。如果为**直接字节缓冲区**，则 Java  虚拟机会尽最大努力直接在此缓冲区上执行本机 I/O  操作。也就是说，在每次调用基础操作系统的一个本机 I/O  操作之前（或之后），虚拟机都会尽量避免将缓冲区的内容复制到中间缓冲区中（或从中间缓冲区中复制内容）。
- 直接字节缓冲区可以通过调用此类的 **allocateDirect()**  工厂方法 来创建。此方法返回的 缓冲区进行分配和取消分配所需**成本通常高于非直接缓冲区** 。直接缓冲区的内容可以驻留在常规的垃圾回收堆之外，因此，它们对应用程序的内存需求量造成的影响可能并不明显。所以，建议将直接缓冲区主要分配给那些易受基础系统的 本机 I/O  操作影响的大型、持久的缓冲区。一般情况下，最好仅在直接缓冲区能在程序性能方面带来明显好处时分配它们。
- 直接字节缓冲区还可以过 通过FileChannel  的 map()  方法  将文件区域直接映射到内存中来创建 。该方法返回MappedByteBuffer  。Java  平台的实现有助于通过 JNI  从本机代码创建直接字节缓冲区。如果以上这些缓冲区中的某个缓冲区实例指的是不可访问的内存区域，则试图访问该区域不会更改该缓冲区的内容，并且将会在访问期间或稍后的某个时间导致抛出不确定的异常。
- 字节缓冲区是直接缓冲区还是非直接缓冲区可通过调用其 **isDirect()**  方法来确定。提供此方法是为了能够在
  性能关键型代码中执行显式缓冲区管理 

## 通道

### 介绍

通道( Channel) :由java.nio.channels包定义的。Channel表示I0源与目标打开的**连接**。Channel类似于传统的“流”。只不过**Channel本身不能直接访问数据**，**Channel 只能与Buffer进行交互**。

### DMA

早先  DMA  直接存储器访问

DMA是在专门的硬件（DMA）控制下，实现高速外设和主存储器之间自动成批交换数据尽量减少CPU干预的输入/输出操作方式

大量读写请求会造成DMA总线间的冲突

![image-20210315093205004](C:\Users\夜神\AppData\Roaming\Typora\typora-user-images\image-20210315093205004.png)

### channel

后来 channel  完全独立的专门用于IO操作的处理器，有自己的指令，附属于CPU中央处理器

![image-20210315093333828](C:\Users\夜神\AppData\Roaming\Typora\typora-user-images\image-20210315093333828.png)

### DMA与channel对比

I/O通道控制方式与DMA方式的bai异同点：
　　**通道控制**（Channel Control）方式与DMA方式类似，也是一种以内存为中心，实现设备和内存直接交换数据的控制方式。
　　与**DMA**方式不同的是，**在DMA方式中**，数据的传送方向、存放数据的内存始址以及传送的数据块长度等都由CPU控制，而**在通道方式中**，这些都由通道来进行控制。另外，DMA方式每台设备至少需要一个DMA控制器，一个通道控制器可以控制多台设备。

**通道是在DMA的基础上增加了能执行有限通道指令的I/O控制器，代替CPU管理控制外设**。

### 实现类  （通道）

java为channel主要提供了一下实现类：

• FileChannel：用于读取、写入、映射和操作文件的通道。

• SocketChannel：通过 TCP 读写网络中的数据。

• ServerSocketChannel：可以监听新进来的 TCP 连接，对每一个新进来
的连接都会创建一个 SocketChannel。

• DatagramChannel：通过 UDP 读写网络中的数据通道。

### 获取方式

获取通道的一种方式是对支持通道的对象调用**getChannel()** 方法。支持通道的类如下：

-  FileInputStream

-  FileOutputStream

-  RandomAccessFile

-  DatagramSocket

-  Socket

-  ServerSocket

  

  JDK1.7 NIO2

  使用 Files 类的静态方法 newByteChannel() 获取字节通道。

  通过通道的静态方法 open() 打开并返回指定通道。

### 直接用通道间的数据传输
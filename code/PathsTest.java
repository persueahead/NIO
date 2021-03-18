package com.spring.demo.test.niotest;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Paths只能从文件系统获取，怎么解决？
 */
public class PathsTest {
    /**
     * TODO 未解决怎么获取类路径下的，或者不应该从类路径下获取
     * @throws IOException
     */
    @Test
    public void pathsTest() throws IOException {
        Path path = Paths.get(".");
        System.out.println(path);
    }
}

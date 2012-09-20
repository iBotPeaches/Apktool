/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.util;

import org.junit.*;

import java.io.File;

public class PathUtilTest {

    @Test
    public void pathUtilTest1() {
        File[] roots = File.listRoots();

        if (roots.length > 1) {
            File basePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar + "test.txt");
            File relativePath = new File(roots[1] + "some" + File.separatorChar + "dir" + File.separatorChar + "test.txt");

            String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

            Assert.assertEquals(path, relativePath.getPath());
        }
    }

    @Test
    public void pathUtilTest2() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar + "test.txt");
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar + "test.txt");

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        /*the "internal" version of the method in PathUtil doesn't handle the case when the "leaf" of the base path
        is a file, this is handled by the two public wrappers. Since we're not calling them, the correct return is
        a single dot"*/
        Assert.assertEquals(path, ".");
    }

    @Test
    public void pathUtilTest3() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar);
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar);

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, ".");
    }

    @Test
    public void pathUtilTest4() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some" + File.separatorChar + "dir");
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir");

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, ".");
    }

    @Test
    public void pathUtilTest5() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some" + File.separatorChar + "dir");
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar);

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, ".");
    }

    @Test
    public void pathUtilTest6() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar);
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir");

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, ".");
    }

    @Test
    public void pathUtilTest7() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some");
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir");

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, "dir");
    }

    @Test
    public void pathUtilTest8() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some" + File.separatorChar);
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar);

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, "dir");
    }

    @Test
    public void pathUtilTest9() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some");
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar);

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, "dir");
    }

    @Test
    public void pathUtilTest10() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some" + File.separatorChar);
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir");

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, "dir");
    }

    @Test
    public void pathUtilTest11() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some");
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar + "dir2");

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, "dir" + File.separatorChar + "dir2");
    }

    @Test
    public void pathUtilTest12() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some" + File.separatorChar);
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar + "dir2" + File.separatorChar);

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, "dir" + File.separatorChar + "dir2");
    }

    @Test
    public void pathUtilTest13() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some");
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar + "dir2" + File.separatorChar);

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, "dir" + File.separatorChar + "dir2");
    }

    @Test
    public void pathUtilTest14() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some" + File.separatorChar);
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar + "dir2");

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, "dir" + File.separatorChar + "dir2");
    }

    @Test
    public void pathUtilTest15() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some" + File.separatorChar + "dir3");
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar + "dir2");

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, ".." + File.separatorChar + "dir" + File.separatorChar + "dir2");
    }

    @Test
    public void pathUtilTest16() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some2" + File.separatorChar + "dir3");
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar + "dir2");

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, ".." + File.separatorChar + ".." + File.separatorChar + "some" + File.separatorChar + "dir" + File.separatorChar + "dir2");
    }

    @Test
    public void pathUtilTest17() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0].getPath());
        File relativePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar + "dir2");

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, "some" + File.separatorChar + "dir" + File.separatorChar + "dir2");
    }

    @Test
    public void pathUtilTest18() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some" + File.separatorChar + "dir");
        File relativePath = new File(roots[0] + "some");

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, "..");
    }

    @Test
    public void pathUtilTest19() {
        File[] roots = File.listRoots();

        File basePath = new File(roots[0] + "some" + File.separatorChar + "dir" + File.separatorChar + "dir2");
        File relativePath = new File(roots[0] + "some");

        String path = PathUtil.getRelativeFileInternal(basePath, relativePath);

        Assert.assertEquals(path, ".." + File.separatorChar + "..");
    }
}

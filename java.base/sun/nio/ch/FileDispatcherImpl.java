/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.JavaIOFileDescriptorAccess;
import sun.security.action.GetPropertyAction;
import java.io.File;
import java.nio.CharBuffer;

class FileDispatcherImpl extends FileDispatcher {

    private static final JavaIOFileDescriptorAccess fdAccess =
        SharedSecrets.getJavaIOFileDescriptorAccess();

    // set to true if fast file transmission (TransmitFile) is enabled
    private static final boolean fastFileTransfer;

    FileDispatcherImpl() { }

    @Override
    boolean needsPositionLock() {
        return true;
    }

    int read(FileDescriptor fd, long address, int len)
        throws IOException
    {
        return read0(fd, address, len);
    }

    int pread(FileDescriptor fd, long address, int len, long position)
        throws IOException
    {
        return pread0(fd, address, len, position);
    }

    long readv(FileDescriptor fd, long address, int len) throws IOException {
        return readv0(fd, address, len);
    }

    int write(FileDescriptor fd, long address, int len) throws IOException {
        return write0(fd, address, len, fdAccess.getAppend(fd));
    }

    int pwrite(FileDescriptor fd, long address, int len, long position)
        throws IOException
    {
        return pwrite0(fd, address, len, position);
    }

    long writev(FileDescriptor fd, long address, int len) throws IOException {
        return writev0(fd, address, len, fdAccess.getAppend(fd));
    }

    long seek(FileDescriptor fd, long offset) throws IOException {
        return seek0(fd, offset);
    }

    int force(FileDescriptor fd, boolean metaData) throws IOException {
        return force0(fd, metaData);
    }

    int truncate(FileDescriptor fd, long size) throws IOException {
        return truncate0(fd, size);
    }

    long size(FileDescriptor fd) throws IOException {
        return size0(fd);
    }

    int lock(FileDescriptor fd, boolean blocking, long pos, long size,
             boolean shared) throws IOException
    {
        return lock0(fd, blocking, pos, size, shared);
    }

    void release(FileDescriptor fd, long pos, long size) throws IOException {
        release0(fd, pos, size);
    }

    void close(FileDescriptor fd) throws IOException {
        fdAccess.close(fd);
    }

    FileDescriptor duplicateForMapping(FileDescriptor fd) throws IOException {
        // on Windows we need to keep a handle to the file
        FileDescriptor result = new FileDescriptor();
        long handle = duplicateHandle(fdAccess.getHandle(fd));
        fdAccess.setHandle(result, handle);
        fdAccess.registerCleanup(result);
        return result;
    }

    boolean canTransferToDirectly(java.nio.channels.SelectableChannel sc) {
        return fastFileTransfer && sc.isBlocking();
    }

    boolean transferToDirectlyNeedsPositionLock() {
        return true;
    }

    int setDirectIO(FileDescriptor fd, String path) {
        int result = -1;
        String filePath = path.substring(0, path.lastIndexOf(File.separator));
        CharBuffer buffer = CharBuffer.allocate(filePath.length());
        buffer.put(filePath);
        try {
            result = setDirect0(fd, buffer);
        } catch (IOException e) {
            throw new UnsupportedOperationException
                ("Error setting up DirectIO", e);
        }
        return result;
    }

    static boolean isFastFileTransferRequested() {
        String fileTransferProp = GetPropertyAction
                .privilegedGetProperty("jdk.nio.enableFastFileTransfer");
        boolean enable;
        if ("".equals(fileTransferProp)) {
            enable = true;
        } else {
            enable = Boolean.parseBoolean(fileTransferProp);
        }
        return enable;
    }

    static {
        IOUtil.load();
        fastFileTransfer = isFastFileTransferRequested();
    }

    //-- Native methods

    static native int read0(FileDescriptor fd, long address, int len)
        throws IOException;

    static native int pread0(FileDescriptor fd, long address, int len,
                             long position) throws IOException;

    static native long readv0(FileDescriptor fd, long address, int len)
        throws IOException;

    static native int write0(FileDescriptor fd, long address, int len, boolean append)
        throws IOException;

    static native int pwrite0(FileDescriptor fd, long address, int len,
                             long position) throws IOException;

    static native long writev0(FileDescriptor fd, long address, int len, boolean append)
        throws IOException;

    static native long seek0(FileDescriptor fd, long offset) throws IOException;

    static native int force0(FileDescriptor fd, boolean metaData)
        throws IOException;

    static native int truncate0(FileDescriptor fd, long size)
        throws IOException;

    static native long size0(FileDescriptor fd) throws IOException;

    static native int lock0(FileDescriptor fd, boolean blocking, long pos,
                            long size, boolean shared) throws IOException;

    static native void release0(FileDescriptor fd, long pos, long size)
        throws IOException;

    static native void close0(FileDescriptor fd) throws IOException;

    static native long duplicateHandle(long fd) throws IOException;

    static native int setDirect0(FileDescriptor fd, CharBuffer buffer) throws IOException;
}

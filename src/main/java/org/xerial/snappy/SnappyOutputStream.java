/*--------------------------------------------------------------------------
 *  Copyright 2011 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
//--------------------------------------
// XerialJ
//
// SnappyOutputStream.java
// Since: 2011/03/31 17:44:10
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import org.xerial.snappy.buffer.BufferAllocatorFactory;
import org.xerial.snappy.buffer.BufferAllocator;
import org.xerial.snappy.buffer.CachedBufferAllocator;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class implements a stream filter for writing compressed data using
 * Snappy.
 * <p>
 * The input data is blocked into 32kb size (in default), and each block is
 * compressed and then passed to the given {@link OutputStream}.
 * </p>
 * The output data format is:
 * <ol>
 * <li>snappy codec header defined in {@link SnappyCodec} (8 bytes)
 * <li>compressed block 1 : a pair of (compressed data size [4 byte integer.
 * Big-endian], compressed data...)
 * <li>compressed block 2
 * <li>...
 * </ol>
 * <p/>
 * Note that the compressed data created by {@link SnappyOutputStream} cannot be
 * uncompressed by {@link Snappy#uncompress(byte[])} since the output formats of
 * {@link Snappy#compress(byte[])} and {@link SnappyOutputStream} are different.
 * Use {@link SnappyInputStream} for uncompress the data generated by
 * {@link SnappyOutputStream}.
 *
 * @author leo
 */
public class SnappyOutputStream extends OutputStream {
    static final int MIN_BLOCK_SIZE = 1 * 1024;
    static final int DEFAULT_BLOCK_SIZE = 32 * 1024; // Use 32kb for the default block size

    protected final OutputStream out;
    private final int blockSize;

    private final BufferAllocator inputBufferAllocator;
    private final BufferAllocator outputBufferAllocator;

    protected final byte[] inputBuffer;
    protected final byte[] outputBuffer;
    private int inputCursor = 0;
    private int outputCursor = 0;

    public SnappyOutputStream(OutputStream out) {
        this(out, DEFAULT_BLOCK_SIZE);
    }

    /**
     * @param out
     * @param blockSize byte size of the internal buffer size
     * @throws IOException
     */
    public SnappyOutputStream(OutputStream out, int blockSize) {
        this(out, blockSize, CachedBufferAllocator.factory);
    }

    public SnappyOutputStream(OutputStream out, int blockSize, BufferAllocatorFactory bufferAllocatorFactory) {
        this.out = out;
        this.blockSize = Math.max(MIN_BLOCK_SIZE, blockSize);
        int inputSize = blockSize;
        int outputSize = SnappyCodec.HEADER_SIZE + 4 + Snappy.maxCompressedLength(blockSize);

        this.inputBufferAllocator = bufferAllocatorFactory.getBufferAllocator(inputSize);
        this.outputBufferAllocator = bufferAllocatorFactory.getBufferAllocator(outputSize);

        inputBuffer = inputBufferAllocator.allocate(inputSize);
        outputBuffer = inputBufferAllocator.allocate(outputSize);

        outputCursor = SnappyCodec.currentHeader.writeHeader(outputBuffer, 0);
    }


    /* (non-Javadoc)
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        rawWrite(b, off, len);
    }

    /**
     * Compress the input long array data
     *
     * @param d   input array
     * @param off offset in the array
     * @param len the number of elements in the array to copy
     * @throws IOException
     */
    public void write(long[] d, int off, int len) throws IOException {
        rawWrite(d, off * 8, len * 8);
    }

    /**
     * Compress the input double array data
     *
     * @param f   input array
     * @param off offset in the array
     * @param len the number of elements in the array to copy
     * @throws IOException
     */
    public void write(double[] f, int off, int len) throws IOException {
        rawWrite(f, off * 8, len * 8);
    }

    /**
     * Compress the input float array data
     *
     * @param f   input array
     * @param off offset in the array
     * @param len the number of elements in the array to copy
     * @throws IOException
     */
    public void write(float[] f, int off, int len) throws IOException {
        rawWrite(f, off * 4, len * 4);
    }

    /**
     * Compress the input int array data
     *
     * @param f   input array
     * @param off offset in the array
     * @param len the number of elements in the array to copy
     * @throws IOException
     */
    public void write(int[] f, int off, int len) throws IOException {
        rawWrite(f, off * 4, len * 4);
    }

    /**
     * Compress the input short array data
     *
     * @param f   input array
     * @param off offset in the array
     * @param len the number of elements in the array to copy
     * @throws IOException
     */
    public void write(short[] f, int off, int len) throws IOException {
        rawWrite(f, off * 2, len * 2);
    }

    /**
     * Compress the input array data
     *
     * @param d
     * @throws IOException
     */
    public void write(long[] d) throws IOException {
        write(d, 0, d.length);
    }

    /**
     * Compress the input array data
     *
     * @param f
     * @throws IOException
     */
    public void write(double[] f) throws IOException {
        write(f, 0, f.length);
    }

    /**
     * Compress the input array data
     *
     * @param f
     * @throws IOException
     */
    public void write(float[] f) throws IOException {
        write(f, 0, f.length);
    }

    /**
     * Compress the input array data
     *
     * @param f
     * @throws IOException
     */
    public void write(int[] f) throws IOException {
        write(f, 0, f.length);
    }

    /**
     * Compress the input array data
     *
     * @param f
     * @throws IOException
     */
    public void write(short[] f) throws IOException {
        write(f, 0, f.length);
    }

    private boolean hasSufficientOutputBufferFor(int inputSize) {
        int maxCompressedSize = Snappy.maxCompressedLength(inputSize);
        return maxCompressedSize < outputBuffer.length - outputCursor - 4;
    }

    /**
     * Compress the raw byte array data.
     *
     * @param array      array data of any type (e.g., byte[], float[], long[], ...)
     * @param byteOffset
     * @param byteLength
     * @throws IOException
     */
    public void rawWrite(Object array, int byteOffset, int byteLength) throws IOException {

        if(inputCursor + byteLength < MIN_BLOCK_SIZE) {
            // copy the input data to uncompressed buffer
            Snappy.arrayCopy(array, byteOffset, byteLength, inputBuffer, inputCursor);
            inputCursor += byteLength;
            return;
        }

        compressInput();

        for(int readBytes = 0; readBytes < byteLength; ) {
            int inputLen = Math.min(blockSize, byteLength - readBytes);
            if(!hasSufficientOutputBufferFor(inputLen)) {
                dumpOutput();
            }
            int compressedSize = Snappy.rawCompress(array, byteOffset + readBytes, inputLen, outputBuffer, outputCursor + 4);
            writeInt(outputBuffer, outputCursor, compressedSize);
            outputCursor += 4 + compressedSize;
            readBytes += inputLen;
        }
    }

    /**
     * Writes the specified byte to this output stream. The general contract for
     * write is that one byte is written to the output stream. The byte to be
     * written is the eight low-order bits of the argument b. The 24 high-order
     * bits of b are ignored.
     */
    /* (non-Javadoc)
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public void write(int b) throws IOException {
        if(inputCursor >= inputBuffer.length) {
            compressInput();
        }
        inputBuffer[inputCursor++] = (byte) b;
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#flush()
     */
    @Override
    public void flush() throws IOException {
        compressInput();
        dumpOutput();
        out.flush();
    }

    static void writeInt(byte[] dst, int offset, int v) {
        dst[offset] = (byte) ((v >> 24) & 0xFF);
        dst[offset + 1] = (byte) ((v >> 16) & 0xFF);
        dst[offset + 2] = (byte) ((v >> 8) & 0xFF);
        dst[offset + 3] = (byte) ((v >> 0) & 0xFF);
    }

    static int readInt(byte[] buffer, int pos) {
        int b1 = (buffer[pos] & 0xFF) << 24;
        int b2 = (buffer[pos + 1] & 0xFF) << 16;
        int b3 = (buffer[pos + 2] & 0xFF) << 8;
        int b4 = buffer[pos + 3] & 0xFF;
        return b1 | b2 | b3 | b4;
    }

    protected void dumpOutput() throws IOException {
        if(outputCursor > 0) {
            out.write(outputBuffer, 0, outputCursor);
            outputCursor = 0;
        }
    }

    protected void compressInput() throws IOException {
        if(inputCursor <= 0) {
            return; // no need to dump
        }

        // Compress and dump the buffer content
        if(!hasSufficientOutputBufferFor(inputCursor)) {
            dumpOutput();
        }
        int compressedSize = Snappy.compress(inputBuffer, 0, inputCursor, outputBuffer, outputCursor + 4);
        // Write compressed data size
        writeInt(outputBuffer, outputCursor, compressedSize);
        outputCursor += 4 + compressedSize;
        inputCursor = 0;
    }

    /**
     * close the stream
     */
    /* (non-Javadoc)
     * @see java.io.OutputStream#close()
     */
    @Override
    public void close() throws IOException {
        try {
            flush();
            out.close();
        } finally {
            inputBufferAllocator.release(inputBuffer);
            outputBufferAllocator.release(outputBuffer);
        }
    }

}

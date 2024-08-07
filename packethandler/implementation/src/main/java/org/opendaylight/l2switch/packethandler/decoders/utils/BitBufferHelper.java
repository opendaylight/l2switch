/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders.utils;

import java.util.Arrays;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BitBufferHelper class that provides utility methods to - fetch specific bits
 * from a serialized stream of bits - convert bits to primitive data type - like
 * short, int, long - store bits in specified location in stream of bits -
 * convert primitive data types to stream of bits.
 */
public final class BitBufferHelper {
    private static final Logger LOG = LoggerFactory.getLogger(BitBufferHelper.class);

    public static final long BYTE_MASK = 0xFF;

    private BitBufferHelper() {
    }

    // Getters
    // data: array where data are stored
    // startOffset: bit from where to start reading
    // numBits: number of bits to read
    // All this function return an exception if overflow or underflow

    /**
     * Returns the first byte from the byte array.
     *
     * @param data the byte array
     * @return byte value
     */
    public static byte getByte(byte[] data) {
        if (data.length * NetUtils.NUM_BITS_IN_A_BYTE > Byte.SIZE) {
            LOG.error("getByte", new BufferException("Container is too small for the number of requested bits"));
        }
        return data[0];
    }

    /**
     * Returns the Uint8 value for the byte array passed. Size of byte array is
     * restricted to 8 bits (1 byte).
     *
     * @param data the byte array
     * @return Uint8 value
     */
    public static Uint8 getUint8(byte[] data) {
        if (data.length * NetUtils.NUM_BITS_IN_A_BYTE > 8) {
            LOG.error("getUint8",
                    new BufferException("Container is too small for the number of requested bits"));
        }
        return Uint8.valueOf(toNumber(data));
    }

    /**
     * Returns the Uint16 value for the byte array passed. Size of byte array is
     * restricted to 16 bits (2 bytes).
     *
     * @param data the byte array
     * @return Uint16 value
     */
    public static Uint16 getUint16(byte[] data) {
        if (data.length * NetUtils.NUM_BITS_IN_A_BYTE > 16) {
            LOG.error("getUint16",
                    new BufferException("Container is too small for the number of requested bits"));
        }
        return Uint16.valueOf(toNumber(data));
    }

    /**
     * Returns the Uint16 value for the last numBits of the byte array passed.
     * Size of numBits is restricted to 16 bits (2 bytes).
     *
     * @param data the byte array
     * @param numBits last number of bits
     * @return the Uint16 value of byte array
     */
    public static Uint16 getUint16(byte[] data, int numBits) {
        if (numBits > 16) {
            LOG.error("getUint16",
                    new BufferException("Container is too small for the number of requested bits"));
        }
        int startOffset = data.length * NetUtils.NUM_BITS_IN_A_BYTE - numBits;
        byte[] bits = null;
        try {
            bits = getBits(data, startOffset, numBits);
        } catch (BufferException e) {
            LOG.error("getUint16 failed", e);
            return Uint16.ZERO;
        }
        return Uint16.valueOf(toNumber(bits, numBits));
    }

    /**
     * Returns the Uint32 value for the byte array passed. Size of byte array is
     * restricted to 32 bits (4 bytes).
     *
     * @param data the byte array
     * @return Uint32 value
     */
    public static Uint32 getUint32(byte[] data) {
        if (data.length * NetUtils.NUM_BITS_IN_A_BYTE > 32) {
            LOG.error("getUint32",
                    new BufferException("Container is too small for the number of requested bits"));
        }
        return Uint32.valueOf(toNumber(data));
    }

    /**
     * Returns the short value for the last numBits of the byte array passed.
     * Size of numBits is restricted to Short.SIZE.
     *
     * @param data the byte array
     * @param numBits last number of bits
     * @return the short value of byte array
     * @deprecated Use {@link #getUint16(byte[], int)} instead.
     */
    @Deprecated(forRemoval = true)
    public static short getShort(byte[] data, int numBits) {
        if (numBits > Short.SIZE) {
            LOG.error("getShort", new BufferException("Container is too small for the number of requested bits"));
        }
        int startOffset = data.length * NetUtils.NUM_BITS_IN_A_BYTE - numBits;
        byte[] bits = null;
        try {
            bits = getBits(data, startOffset, numBits);
        } catch (BufferException e) {
            LOG.error("getBits failed", e);
            return 0;
        }
        return (short) toNumber(bits, numBits);
    }

    /**
     * Returns the short value for the byte array passed. Size of byte array is
     * restricted to Short.SIZE.
     *
     * @param data the byte array
     * @return short value
     */
    public static short getShort(byte[] data) {
        if (data.length > Short.SIZE) {
            LOG.error("getShort", new BufferException("Container is too small for the number of requested bits"));
        }
        return (short) toNumber(data);
    }

    /**
     * Returns the int value for the last numBits of the byte array passed. Size
     * of numBits is restricted to Integer.SIZE.
     *
     * @param data the byte array
     * @param numBits the number of bits
     * @return the integer value of byte array
     * @deprecated Use {@link #getUint16(byte[], int)} instead.
     */
    @Deprecated(forRemoval = true)
    public static int getInt(byte[] data, int numBits) {
        if (numBits > Integer.SIZE) {
            LOG.error("getInt", new BufferException("Container is too small for the number of requested bits"));
        }
        int startOffset = data.length * NetUtils.NUM_BITS_IN_A_BYTE - numBits;
        byte[] bits = null;
        try {
            bits = BitBufferHelper.getBits(data, startOffset, numBits);
        } catch (BufferException e) {
            LOG.error("getBits failed", e);
            return 0;
        }
        return (int) toNumber(bits, numBits);
    }

    /**
     * Returns the int value for the byte array passed. Size of byte array is
     * restricted to Integer.SIZE.
     *
     * @param data the byte array
     * @return int - the integer value of byte array
     */
    public static int getInt(byte[] data) {
        if (data.length > Integer.SIZE) {
            LOG.error("getInt", new BufferException("Container is too small for the number of requested bits"));
        }
        return (int) toNumber(data);
    }

    /**
     * Returns the long value for the last numBits of the byte array passed.
     * Size of numBits is restricted to Long.SIZE.
     *
     * @param data the byte array
     * @param numBits the number of bits
     * @return the integer value of byte array
     * @deprecated Delete this method since it was not used
     */
    @Deprecated(forRemoval = true)
    public static long getLong(byte[] data, int numBits) {
        if (numBits > Long.SIZE) {
            LOG.error("getLong", new BufferException("Container is too small for the number of requested bits"));
        }
        if (numBits > data.length * NetUtils.NUM_BITS_IN_A_BYTE) {
            LOG.error("getLong", new BufferException("Trying to read more bits than contained in the data buffer"));
        }
        int startOffset = data.length * NetUtils.NUM_BITS_IN_A_BYTE - numBits;
        byte[] bits = null;
        try {
            bits = BitBufferHelper.getBits(data, startOffset, numBits);
        } catch (BufferException e) {
            LOG.error("getBits failed", e);
            return 0;
        }
        return toNumber(bits, numBits);
    }

    /**
     * Returns the long value for the byte array passed. Size of byte array is
     * restricted to Long.SIZE.
     *
     * @param data the byte array
     * @return long - the integer value of byte array
     * @deprecated Delete this method since it was not used
     */
    @Deprecated(forRemoval = true)
    public static long getLong(byte[] data) {
        if (data.length > Long.SIZE) {
            LOG.error("getLong", new BufferException("Container is too small for the number of requested bits"));
        }
        return toNumber(data);
    }

    /**
     * Reads the specified number of bits from the passed byte array starting to
     * read from the specified offset The bits read are stored in a byte array
     * which size is dictated by the number of bits to be stored. The bits are
     * stored in the byte array LSB aligned.
     *
     * <p>
     * Ex. Read 7 bits at offset 10 0 9 10 16 17 0101000010 | 0000101 |
     * 1111001010010101011 will be returned as {0,0,0,0,0,1,0,1}
     *
     * @param data the byte array
     * @param startOffset
     *            offset to start fetching bits from data from
     * @param numBits
     *            number of bits to be fetched from data
     * @return LSB aligned bits
     * @throws BufferException
     *             when the startOffset and numBits parameters are not congruent
     *             with the data buffer size
     */
    public static byte[] getBits(byte[] data, int startOffset, int numBits) throws BufferException {

        int startByteOffset = 0;
        final int extranumBits = numBits % NetUtils.NUM_BITS_IN_A_BYTE;
        final int extraOffsetBits = startOffset % NetUtils.NUM_BITS_IN_A_BYTE;
        int numBytes = numBits % NetUtils.NUM_BITS_IN_A_BYTE != 0 ? 1 + numBits / NetUtils.NUM_BITS_IN_A_BYTE
                : numBits / NetUtils.NUM_BITS_IN_A_BYTE;
        startByteOffset = startOffset / NetUtils.NUM_BITS_IN_A_BYTE;
        byte[] bytes = new byte[numBytes];
        if (numBits == 0) {
            return bytes;
        }

        checkExceptions(data, startOffset, numBits);

        if (extraOffsetBits == 0) {
            if (extranumBits == 0) {
                System.arraycopy(data, startByteOffset, bytes, 0, numBytes);
                return bytes;
            } else {
                System.arraycopy(data, startByteOffset, bytes, 0, numBytes - 1);
                bytes[numBytes - 1] = (byte) (data[startByteOffset + numBytes - 1] & getMSBMask(extranumBits));
            }
        } else {
            int index;
            int valfromnext;
            for (index = 0; index < numBits / NetUtils.NUM_BITS_IN_A_BYTE; index++) {
                // Reading numBytes starting from offset
                int valfromcurr = data[startByteOffset + index]
                        & getLSBMask(NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits);
                valfromnext = data[startByteOffset + index + 1] & getMSBMask(extraOffsetBits);
                bytes[index] = (byte) (valfromcurr << extraOffsetBits
                        | valfromnext >> NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits);
            }
            // Now adding the rest of the bits if any
            if (extranumBits != 0) {
                if (extranumBits < NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits) {
                    valfromnext = (byte) (data[startByteOffset + index]
                            & getMSBMask(extranumBits) >> extraOffsetBits);
                    bytes[index] = (byte) (valfromnext << extraOffsetBits);
                } else if (extranumBits == NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits) {
                    int valfromcurr = data[startByteOffset + index]
                            & getLSBMask(NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits);
                    bytes[index] = (byte) (valfromcurr << extraOffsetBits);
                } else {
                    int valfromcurr = data[startByteOffset + index]
                            & getLSBMask(NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits);
                    valfromnext = data[startByteOffset + index + 1]
                            & getMSBMask(extranumBits - (NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits));
                    bytes[index] = (byte) (valfromcurr << extraOffsetBits
                            | valfromnext >> NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits);
                }

            }
        }
        // Aligns the bits to LSB
        return shiftBitsToLSB(bytes, numBits);
    }

    // Setters
    // data: array where data will be stored
    // input: the data that need to be stored in the data array
    // startOffset: bit from where to start writing
    // numBits: number of bits to read

    /**
     * Bits are expected to be stored in the input byte array from LSB.
     *
     * @param data
     *            data to set the input byte
     * @param input
     *            byte to be inserted
     * @param startOffset
     *            offset in data to start inserting byte from
     * @param numBits
     *            number of bits of input to be inserted into data[]
     * @throws BufferException
     *             when the input, startOffset and numBits are not congruent
     *             with the data buffer size
     */
    public static void setByte(byte[] data, byte input, int startOffset, int numBits) throws BufferException {
        byte[] inputByteArray = new byte[1];
        Arrays.fill(inputByteArray, 0, 1, input);
        setBytes(data, inputByteArray, startOffset, numBits);
    }

    /**
     * Bits are expected to be stored in the input byte array from LSB.
     *
     * @param data
     *            data to set the input byte
     * @param input
     *            input bytes to be inserted
     * @param startOffset
     *            offset of data[] to start inserting byte from
     * @param numBits
     *            number of bits of input to be inserted into data[]
     * @throws BufferException
     *             when the startOffset and numBits parameters are not congruent
     *             with data and input buffers' size
     */
    public static void setBytes(byte[] data, byte[] input, int startOffset, int numBits) throws BufferException {
        checkExceptions(data, startOffset, numBits);
        insertBits(data, input, startOffset, numBits);
    }

    /**
     * Returns numBits 1's in the MSB position.
     */
    public static int getMSBMask(int numBits) {
        int mask = 0;
        for (int i = 0; i < numBits; i++) {
            mask = mask | 1 << 7 - i;
        }
        return mask;
    }

    /**
     * Returns numBits 1's in the LSB position.
     */
    public static int getLSBMask(int numBits) {
        int mask = 0;
        for (int i = 0; i < numBits; i++) {
            mask = mask | 1 << i;
        }
        return mask;
    }

    /**
     * Returns the numerical value of the byte array passed.
     */
    public static long toNumber(byte[] array) {
        long ret = 0;
        long length = array.length;
        int value = 0;
        for (int i = 0; i < length; i++) {
            value = array[i];
            if (value < 0) {
                value += 256;
            }
            ret = ret | (long) value << (length - i - 1) * NetUtils.NUM_BITS_IN_A_BYTE;
        }
        return ret;
    }

    /**
     * Returns the numerical value of the last numBits (LSB bits) of the byte
     * array passed.
     *
     * @param array the byte array
     * @param numBits the number of bits
     * @return numerical value of byte array passed
     */
    public static long toNumber(byte @NonNull [] array, int numBits) {
        int length = numBits / NetUtils.NUM_BITS_IN_A_BYTE;
        int bitsRest = numBits % NetUtils.NUM_BITS_IN_A_BYTE;
        int startOffset = array.length - length;
        long ret = 0;
        int value = 0;

        value = array[startOffset - 1] & getLSBMask(bitsRest);
        value = array[startOffset - 1] < 0 ? array[startOffset - 1] + 256 : array[startOffset - 1];
        ret = ret | value << (array.length - startOffset) * NetUtils.NUM_BITS_IN_A_BYTE;

        for (int i = startOffset; i < array.length; i++) {
            value = array[i];
            if (value < 0) {
                value += 256;
            }
            ret = ret | (long) value << (array.length - i - 1) * NetUtils.NUM_BITS_IN_A_BYTE;
        }

        return ret;
    }

    /**
     * Accepts a number as input and returns its value in byte form in LSB
     * aligned form example: input = 5000 [1001110001000] bytes = 19, -120
     * [00010011] [10001000].
     */

    public static byte[] toByteArray(Number input) {
        Class<? extends Number> dataType = input.getClass();
        short size = 0;
        long longValue = input.longValue();

        if (dataType == Byte.class || dataType == byte.class) {
            size = Byte.SIZE;
        } else if (dataType == Short.class || dataType == short.class) {
            size = Short.SIZE;
        } else if (dataType == Integer.class || dataType == int.class) {
            size = Integer.SIZE;
        } else if (dataType == Long.class || dataType == long.class) {
            size = Long.SIZE;
        } else {
            throw new IllegalArgumentException("Parameter must one of the following: Short/Int/Long\n");
        }

        int length = size / NetUtils.NUM_BITS_IN_A_BYTE;
        byte[] bytes = new byte[length];

        // Getting the bytes from input value
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (longValue >> NetUtils.NUM_BITS_IN_A_BYTE * (length - i - 1) & BYTE_MASK);
        }
        return bytes;
    }

    /**
     * Accepts a number as input and returns its value in byte form in MSB
     * aligned form example: input = 5000 [1001110001000] bytes = -114, 64
     * [10011100] [01000000].
     *
     * @param input inout Number
     * @param numBits
     *            the number of bits to be returned
     * @return byte[] a byte array
     */
    public static byte[] toByteArray(Number input, int numBits) {
        Class<? extends Number> dataType = input.getClass();
        short size = 0;
        long longValue = input.longValue();

        if (dataType == Short.class) {
            size = Short.SIZE;
        } else if (dataType == Integer.class) {
            size = Integer.SIZE;
        } else if (dataType == Long.class) {
            size = Long.SIZE;
        } else {
            throw new IllegalArgumentException("Parameter must one of the following: Short/Int/Long\n");
        }

        int length = size / NetUtils.NUM_BITS_IN_A_BYTE;
        byte[] bytes = new byte[length];
        byte[] inputbytes = new byte[length];
        byte[] shiftedBytes;

        // Getting the bytes from input value
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (longValue >> NetUtils.NUM_BITS_IN_A_BYTE * (length - i - 1) & BYTE_MASK);
        }

        if (bytes[0] == 0 && dataType == Long.class || bytes[0] == 0 && dataType == Integer.class) {
            int index = 0;
            for (index = 0; index < length; ++index) {
                if (bytes[index] != 0) {
                    bytes[0] = bytes[index];
                    break;
                }
            }
            System.arraycopy(bytes, index, inputbytes, 0, length - index);
            Arrays.fill(bytes, length - index + 1, length - 1, (byte) 0);
        } else {
            System.arraycopy(bytes, 0, inputbytes, 0, length);
        }

        shiftedBytes = shiftBitsToMSB(inputbytes, numBits);

        return shiftedBytes;
    }

    /**
     * It aligns the last numBits bits to the head of the byte array following
     * them with numBits % 8 zero bits.
     *
     * <p>
     * Example: For inputbytes = [00000111][01110001] and numBits = 12 it
     * returns: shiftedBytes = [01110111][00010000]
     *
     * @param inputBytes the byte array
     * @param numBits
     *            number of bits to be left aligned
     * @return byte[] a byte array
     */
    public static byte[] shiftBitsToMSB(byte[] inputBytes, int numBits) {
        int numBitstoShiftBy = 0;
        int leadZeroesMSB = 8;
        int index;

        for (index = 0; index < Byte.SIZE; index++) {
            if ((byte) (inputBytes[0] & getMSBMask(index + 1)) != 0) {
                leadZeroesMSB = index;
                break;
            }
        }

        if (numBits % NetUtils.NUM_BITS_IN_A_BYTE == 0) {
            numBitstoShiftBy = 0;
        } else {
            numBitstoShiftBy = NetUtils.NUM_BITS_IN_A_BYTE - numBits % NetUtils.NUM_BITS_IN_A_BYTE < leadZeroesMSB
                    ? NetUtils.NUM_BITS_IN_A_BYTE - numBits % NetUtils.NUM_BITS_IN_A_BYTE : leadZeroesMSB;
        }
        if (numBitstoShiftBy == 0) {
            return inputBytes;
        }

        int size = inputBytes.length;
        final byte[] shiftedBytes = new byte[size];
        if (numBits < NetUtils.NUM_BITS_IN_A_BYTE) {
            // inputbytes.length = 1 OR read less than a byte
            shiftedBytes[0] = (byte) ((inputBytes[0] & getLSBMask(numBits)) << numBitstoShiftBy);
        } else {
            // # of bits to read from last byte
            final int numEndRestBits = NetUtils.NUM_BITS_IN_A_BYTE
                    - (inputBytes.length * NetUtils.NUM_BITS_IN_A_BYTE - numBits - numBitstoShiftBy);

            for (index = 0; index < size - 1; index++) {
                if (index + 1 == size - 1) {
                    if (numEndRestBits > numBitstoShiftBy) {
                        shiftedBytes[index] = (byte) (inputBytes[index] << numBitstoShiftBy | (inputBytes[index + 1]
                                & getMSBMask(numBitstoShiftBy)) >> numEndRestBits - numBitstoShiftBy);
                        shiftedBytes[index + 1] = (byte) ((inputBytes[index + 1]
                                & getLSBMask(numEndRestBits - numBitstoShiftBy)) << numBitstoShiftBy);
                    } else {
                        shiftedBytes[index] = (byte) (inputBytes[index] << numBitstoShiftBy | (inputBytes[index + 1]
                                & getMSBMask(numEndRestBits)) >> NetUtils.NUM_BITS_IN_A_BYTE - numEndRestBits);
                    }
                }
                shiftedBytes[index] = (byte) (inputBytes[index] << numBitstoShiftBy | (inputBytes[index + 1]
                        & getMSBMask(numBitstoShiftBy)) >> NetUtils.NUM_BITS_IN_A_BYTE - numBitstoShiftBy);
            }

        }
        return shiftedBytes;
    }

    /**
     * It aligns the first numBits bits to the right end of the byte array
     * preceding them with numBits % 8 zero bits.
     *
     * <p>
     * Example: For inputbytes = [01110111][00010000] and numBits = 12 it
     * returns: shiftedBytes = [00000111][01110001]
     *
     * @param inputBytes the byte array
     * @param numBits
     *            number of bits to be right aligned
     * @return byte[] a byte array
     */
    public static byte[] shiftBitsToLSB(byte[] inputBytes, int numBits) {
        int numBytes = inputBytes.length;
        int numBitstoShift = numBits % NetUtils.NUM_BITS_IN_A_BYTE;
        byte[] shiftedBytes = new byte[numBytes];
        int inputLsb = 0;
        int inputMsb = 0;

        if (numBitstoShift == 0) {
            return inputBytes;
        }

        for (int i = 1; i < numBytes; i++) {
            inputLsb = inputBytes[i - 1] & getLSBMask(NetUtils.NUM_BITS_IN_A_BYTE - numBitstoShift);
            inputLsb = inputLsb < 0 ? inputLsb + 256 : inputLsb;
            inputMsb = inputBytes[i] & getMSBMask(numBitstoShift);
            inputMsb = inputBytes[i] < 0 ? inputBytes[i] + 256 : inputBytes[i];
            shiftedBytes[i] = (byte) (inputLsb << numBitstoShift
                    | inputMsb >> NetUtils.NUM_BITS_IN_A_BYTE - numBitstoShift);
        }
        inputMsb = inputBytes[0] & getMSBMask(numBitstoShift);
        inputMsb = inputMsb < 0 ? inputMsb + 256 : inputMsb;
        shiftedBytes[0] = (byte) (inputMsb >> NetUtils.NUM_BITS_IN_A_BYTE - numBitstoShift);
        return shiftedBytes;
    }

    /**
     * Insert in the data buffer at position dictated by the offset the number
     * of bits specified from the input data byte array. The input byte array
     * has the bits stored starting from the LSB.
     *
     * @param data the byte array
     * @param inputdataLSB LSB array
     * @param startOffset start offset
     * @param numBits the number of bits
     */
    public static void insertBits(byte[] data, byte[] inputdataLSB, int startOffset, int numBits) {
        byte[] inputdata = shiftBitsToMSB(inputdataLSB, numBits); // Align to
        // MSB the
        // passed byte
        // array
        int numBytes = numBits / NetUtils.NUM_BITS_IN_A_BYTE;
        int startByteOffset = startOffset / NetUtils.NUM_BITS_IN_A_BYTE;
        int extraOffsetBits = startOffset % NetUtils.NUM_BITS_IN_A_BYTE;
        int extranumBits = numBits % NetUtils.NUM_BITS_IN_A_BYTE;
        int restBits = numBits % NetUtils.NUM_BITS_IN_A_BYTE;
        int inputMSBbits = 0;
        int inputLSBbits = 0;

        if (numBits == 0) {
            return;
        }

        if (extraOffsetBits == 0) {
            if (extranumBits == 0) {
                numBytes = numBits / NetUtils.NUM_BITS_IN_A_BYTE;
                System.arraycopy(inputdata, 0, data, startByteOffset, numBytes);
            } else {
                System.arraycopy(inputdata, 0, data, startByteOffset, numBytes);
                data[startByteOffset + numBytes] = (byte) (data[startByteOffset + numBytes]
                        | inputdata[numBytes] & getMSBMask(extranumBits));
            }
        } else {
            int index;
            for (index = 0; index < numBytes; index++) {
                if (index != 0) {
                    inputLSBbits = inputdata[index - 1] & getLSBMask(extraOffsetBits);
                }
                inputMSBbits = (byte) (inputdata[index] & getMSBMask(NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits));
                inputMSBbits = inputMSBbits >= 0 ? inputMSBbits : inputMSBbits + 256;
                data[startByteOffset + index] = (byte) (data[startByteOffset + index]
                        | inputLSBbits << NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits
                        | inputMSBbits >> extraOffsetBits);
                inputMSBbits = inputLSBbits = 0;
            }
            if (restBits < NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits) {
                if (numBytes != 0) {
                    inputLSBbits = inputdata[index - 1] & getLSBMask(extraOffsetBits);
                }
                inputMSBbits = (byte) (inputdata[index] & getMSBMask(restBits));
                inputMSBbits = inputMSBbits >= 0 ? inputMSBbits : inputMSBbits + 256;
                data[startByteOffset + index] = (byte) (data[startByteOffset + index]
                        | inputLSBbits << NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits
                        | inputMSBbits >> extraOffsetBits);
            } else if (restBits == NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits) {
                if (numBytes != 0) {
                    inputLSBbits = inputdata[index - 1] & getLSBMask(extraOffsetBits);
                }
                inputMSBbits = (byte) (inputdata[index] & getMSBMask(NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits));
                inputMSBbits = inputMSBbits >= 0 ? inputMSBbits : inputMSBbits + 256;
                data[startByteOffset + index] = (byte) (data[startByteOffset + index]
                        | inputLSBbits << NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits
                        | inputMSBbits >> extraOffsetBits);
            } else {
                if (numBytes != 0) {
                    inputLSBbits = inputdata[index - 1] & getLSBMask(extraOffsetBits);
                }
                inputMSBbits = (byte) (inputdata[index] & getMSBMask(NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits));
                inputMSBbits = inputMSBbits >= 0 ? inputMSBbits : inputMSBbits + 256;
                data[startByteOffset + index] = (byte) (data[startByteOffset + index]
                        | inputLSBbits << NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits
                        | inputMSBbits >> extraOffsetBits);

                inputLSBbits = inputdata[index] & getLSBMask(restBits
                        - (NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits)) << NetUtils.NUM_BITS_IN_A_BYTE - restBits;
                data[startByteOffset + index + 1] = (byte) (data[startByteOffset + index + 1]
                        | inputLSBbits << NetUtils.NUM_BITS_IN_A_BYTE - extraOffsetBits);
            }
        }
    }

    /**
     * Checks for overflow and underflow exceptions.
     *
     * @param data the byte array
     * @param startOffset start offset
     * @param numBits the number of bits
     * @throws BufferException
     *             when the startOffset and numBits parameters are not congruent
     *             with the data buffer's size
     */
    public static void checkExceptions(byte[] data, int startOffset, int numBits) throws BufferException {
        int endOffsetByte;
        int startByteOffset;
        endOffsetByte = startOffset / NetUtils.NUM_BITS_IN_A_BYTE + numBits / NetUtils.NUM_BITS_IN_A_BYTE
                + (numBits % NetUtils.NUM_BITS_IN_A_BYTE != 0 ? 1
                        : startOffset % NetUtils.NUM_BITS_IN_A_BYTE != 0 ? 1 : 0);
        startByteOffset = startOffset / NetUtils.NUM_BITS_IN_A_BYTE;

        if (data == null) {
            throw new BufferException("data[] is null\n");
        }

        if (startOffset < 0 || startByteOffset >= data.length || endOffsetByte > data.length || numBits < 0
                || numBits > NetUtils.NUM_BITS_IN_A_BYTE * data.length) {
            throw new BufferException("Illegal arguement/out of bound exception - data.length = " + data.length
                    + " startOffset = " + startOffset + " numBits " + numBits);
        }
    }
}

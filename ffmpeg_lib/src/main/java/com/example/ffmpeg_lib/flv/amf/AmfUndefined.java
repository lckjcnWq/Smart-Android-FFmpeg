package com.example.ffmpeg_lib.flv.amf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @Version
 */

public class AmfUndefined implements AmfData {
    public static final int SIZE = 1;

    @Override
    public void writeTo(OutputStream out) throws IOException {
        out.write(AmfType.UNDEFINED.getValue());
    }

    @Override
    public void readFrom(InputStream in) throws IOException {
        // Skip data type byte (we assume it's already read)
    }

    public static void writeUndefinedTo(OutputStream out) throws IOException {
        out.write(AmfType.UNDEFINED.getValue());
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public byte[] getBytes() {
        byte[] data = new byte[1];
        data[0] = AmfType.UNDEFINED.getValue();
        return data;
    }
}

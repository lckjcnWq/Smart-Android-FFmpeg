package com.example.ffmpeg_lib.flv.amf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @Version
 */

public interface AmfData {
    /**
     * Write/Serialize this AMF data intance (Object/string/integer etc) to
     * the specified OutputStream
     */
    void writeTo(OutputStream out) throws IOException;

    /**
     * Read and parse bytes from the specified input stream to populate this
     * AMFData instance (deserialize)
     *
     * @return the amount of bytes read
     */
    void readFrom(InputStream in) throws IOException;

    /** @return the amount of bytes required for this object */
    int getSize();

    /** @return the bytes of this object */
    byte[] getBytes();
}

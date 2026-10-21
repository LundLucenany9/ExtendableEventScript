package de.lundlucenany9;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compressor {

    public static byte[] compress(byte[] bytes) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try(GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(bytes);
        }
        byte[] code = baos.toByteArray();
        byte[] prefix = createPrefix();
        byte[] result = new byte[prefix.length + code.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(code, 0, result, prefix.length, code.length);
        return result;
    }

    public static byte[] decompress(byte[] data) throws IOException {
        if (data.length < 7
            || data[0] != 'E'
            || data[1] != 'E'
            || data[2] != 'C'
            || data[3] != 0) {
            throw new IOException("Invalid EEC header or unsupported version");
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] code = new byte[data.length - 4];
        System.arraycopy(data, 4, code, 0, code.length);
        try (GZIPInputStream gzip =
                     new GZIPInputStream(new ByteArrayInputStream(code))) {
            gzip.transferTo(buffer);
        }

        return buffer.toByteArray();
    }
    private static byte[] createPrefix() {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buffer);

            out.writeBytes("EEC\0");
            out.flush();
            return buffer.toByteArray();
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

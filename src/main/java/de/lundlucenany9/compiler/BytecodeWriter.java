package de.lundlucenany9.compiler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BytecodeWriter {
    private final ByteArrayOutputStream buffer;
    private final DataOutputStream out;

    private final List<String> strings = new ArrayList<>();

    public BytecodeWriter() {
        buffer = new ByteArrayOutputStream();
        out = new DataOutputStream(buffer);
    }

    public void writeOpcode(Opcode opcode) {
        writeByte((byte) opcode.ordinal());
    }

    public void writeByte(byte b) {
        try {
            out.writeByte(b);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void writeBytes(byte[] b) {
        try{
            out.write(b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeBytes(String s) {
        try{
            out.writeBytes(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeShort(short s) {
        try {
            out.writeShort(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void writeInt(int i) {
        try {
            out.writeInt(i);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void writeDouble(double d) {
        try {
            out.writeDouble(d);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeString(String s) {
        try {
            int index = strings.indexOf(s);

            if (index == -1) {
                if (strings.size() > 0xFFFF) {
                    throw new CompileException("Too many strings");
                }

                index = strings.size();
                strings.add(s);
            }

            if (index > 0xFFFF) {
                throw new CompileException("String pool index too large");
            }

            out.writeShort(index);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void writeBit(boolean bit) {
        writeByte((byte) (bit ? 1 : 0));
    }

    public int getOffset() {
        return buffer.size();
    }

    public void setIntAt(int offset, int value) {
        byte[] data = buffer.toByteArray();

        if (offset < 0 || offset + 4 > data.length) {
            throw new IndexOutOfBoundsException("Offset: " + offset);
        }

        data[offset]     = (byte) (value >>> 24);
        data[offset + 1] = (byte) (value >>> 16);
        data[offset + 2] = (byte) (value >>> 8);
        data[offset + 3] = (byte) value;

        buffer.reset();
        try {
            buffer.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] createPrefix() {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buffer);

            out.writeBytes("EES\0");
            out.writeByte((byte) 0x01);

            out.writeShort(strings.size());
            for (String s : strings) {
                byte[] bytes = s.getBytes(StandardCharsets.UTF_8);

                out.writeShort(bytes.length);
                out.write(bytes);
            }

            out.flush();
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] toByteArray() {
        try {
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] prefix = createPrefix();
        byte[] body = buffer.toByteArray();
        byte[] result = new byte[prefix.length + body.length];

        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(body, 0, result, prefix.length, body.length);

        return result;
    }
}

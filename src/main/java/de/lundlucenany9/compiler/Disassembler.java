package de.lundlucenany9.compiler;

import de.lundlucenany9.Compressor;
import de.lundlucenany9.runtime.ScriptException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

public class Disassembler {
    public static String disassemble(byte[] code) {
        ByteBuffer buf = ByteBuffer.wrap(code);
        StringBuilder sb = new StringBuilder();

        byte[] magic = new byte[4];
        buf.get(magic);
        if(Arrays.equals(magic, new byte[]{'E', 'E', 'C', 0})) {
            try {
                buf = ByteBuffer.wrap(Compressor.decompress(code));
                buf.get(magic);
            } catch (IOException e) {
                throw new ScriptException("Cannot decompress bytecode");
            }
        }
        if (!Arrays.equals(magic, new byte[]{'E', 'E', 'S', 0})) {
            throw new ScriptException("Invalid bytecode");
        }
        int version = buf.get();
        sb.append("; version ").append(version).append('\n');
        
        List<String> strings = new ArrayList<>();
        int stringsLength = buf.getShort() & 0xFFFF;
        for (int i = 0; i < stringsLength; i++) {
            int len = buf.getShort() & 0xFFFF;
            byte[] b = new byte[len];
            buf.get(b);
            strings.add(new String(b, StandardCharsets.UTF_8));
        }

        try {
            while (buf.hasRemaining()) {
                int pos = buf.position();
                Opcode op = Opcode.values()[buf.get()];
                sb.append(String.format("%04d  %s", pos, op));

                switch (op) {
                    case IMPORT -> sb.append(" ").append(readString(strings, buf));
                    case DEFINE_TMP, UNDEFINE_TMP, STORE_TMP, LOAD, STORE, LOAD_REF, LOAD_NEXT_INDEX -> sb.append(' ').append(readString(strings, buf));
                    case PUSH -> {
                        PushType t = PushType.values()[buf.get()];
                        sb.append(' ').append(t);
                        switch (t) {
                            case INTEGER -> sb.append(' ').append(buf.getInt());
                            case FLOAT -> sb.append(' ').append(buf.getDouble());
                            case STRING -> sb.append(" \"").append(readString(strings, buf)).append('"');
                            case ARRAY, MAP -> sb.append(' ').append(buf.getShort());
                            case BOOLEAN -> sb.append(' ').append(buf.get() == 1);
                            default -> {
                            }
                        }
                    }
                    case DEFINE_VAR -> {
                        boolean ro = buf.get() == 1;      // Flag zuerst, wie im Compiler
                        sb.append(ro ? " readonly " : " ").append(readString(strings, buf));
                    }
                    case JMP, JMP_IF, JMP_IF_NOT -> sb.append(String.format(" @%04d", buf.getInt()));
                    case CALL -> {
                        sb.append(' ').append(readString(strings, buf));
                        sb.append(" args=").append(buf.get());
                        int opt = buf.get();
                        sb.append(" opt=[");
                        for (int i = 0; i < opt; i++) sb.append(i > 0 ? "," : "").append(readString(strings, buf));
                        sb.append(']');
                    }
                    case DEFINE_FUNC -> {
                        sb.append(" ").append(readString(strings, buf));
                        int pc = buf.get();
                        sb.append(" params=[");
                        for (int i = 0; i < pc; i++) {
                            boolean ref = buf.get() == 1;
                            sb.append(i > 0 ? "," : "").append(ref ? "#" : "").append(readString(strings, buf));
                        }
                        sb.append("] opt=[");
                        int oc = buf.get();
                        for (int i = 0; i < oc; i++) sb.append(i > 0 ? "," : "").append(readString(strings, buf));
                        sb.append(String.format("] end=@%04d", buf.getInt()));
                    }
                    default -> {
                    }
                }
                sb.append('\n');
            }
        } catch (Exception e) {
            System.err.println("Error at position " + buf.position());
            System.out.println(sb);
            System.out.println(HexFormat.ofDelimiter(" ").formatHex(buf.array(), buf.position(), buf.limit()));
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    private static String readString(List<String> strings, ByteBuffer buf) {
        return strings.get(buf.getShort() & 0xFFFF);
    }
}

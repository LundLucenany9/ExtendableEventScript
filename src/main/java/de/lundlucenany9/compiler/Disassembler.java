package de.lundlucenany9.compiler;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Disassembler {
    public static String disassemble(byte[] code) {
        ByteBuffer buf = ByteBuffer.wrap(code);
        StringBuilder sb = new StringBuilder();

        byte[] magic = new byte[4];
        buf.get(magic);
        int version = buf.get();
        sb.append("; version ").append(version).append('\n');

        try {
            while (buf.hasRemaining()) {
                int pos = buf.position();
                Opcode op = Opcode.values()[buf.get()];
                sb.append(String.format("%04d  %s", pos, op));

                switch (op) {
                    case IMPORT -> sb.append(" ").append(readString(buf));
                    case DEFINE_TMP, UNDEFINE_TMP, LOAD, STORE, LOAD_REF, LOAD_NEXT_INDEX -> sb.append(' ').append(readString(buf));
                    case PUSH -> {
                        PushType t = PushType.values()[buf.get()];
                        sb.append(' ').append(t);
                        switch (t) {
                            case INTEGER -> sb.append(' ').append(buf.getInt());
                            case FLOAT -> sb.append(' ').append(buf.getDouble());
                            case STRING -> sb.append(" \"").append(readString(buf)).append('"');
                            case ARRAY, MAP -> sb.append(' ').append(buf.getShort());
                            default -> {
                            }
                        }
                    }
                    case DEFINE_VAR -> {
                        boolean ro = buf.get() == 1;      // Flag zuerst, wie im Compiler
                        sb.append(ro ? " readonly " : " ").append(readString(buf));
                    }
                    case JMP, JMP_IF, JMP_IF_NOT -> sb.append(String.format(" @%04d", buf.getInt()));
                    case CALL -> {
                        sb.append(' ').append(readString(buf));
                        sb.append(" args=").append(buf.get());
                        int opt = buf.get();
                        sb.append(" opt=[");
                        for (int i = 0; i < opt; i++) sb.append(i > 0 ? "," : "").append(readString(buf));
                        sb.append(']');
                    }
                    case DEFINE_FUNC -> {
                        sb.append(" ").append(readString(buf));
                        int pc = buf.get();
                        sb.append(" params=[");
                        for (int i = 0; i < pc; i++) {
                            boolean ref = buf.get() == 1;
                            sb.append(i > 0 ? "," : "").append(ref ? "#" : "").append(readString(buf));
                        }
                        sb.append("] opt=[");
                        int oc = buf.get();
                        for (int i = 0; i < oc; i++) sb.append(i > 0 ? "," : "").append(readString(buf));
                        sb.append(String.format("] end=@%04d", buf.getInt()));
                    }
                    default -> {
                    }
                }
                sb.append('\n');
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    private static String readString(ByteBuffer buf) {
        int len = buf.getShort() & 0xFFFF;
        byte[] b = new byte[len];
        buf.get(b);
        return new String(b, StandardCharsets.UTF_8);
    }
}

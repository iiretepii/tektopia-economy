import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.stream.*;
import org.objectweb.asm.*;

/**
 * Lists every external reference (method, field, annotation element) made by the classes in
 * <classesDir> and checks it against the references and declarations found in real mod jars.
 *
 * Usage: RefCheck <classesDir> <prefixToCheck,...> <corpusJar...>
 * Output: OK / OWNER-DIFFERS / MISSING per reference.
 */
public class RefCheck {
    static Set<String> corpus = new HashSet<>();
    static Set<String> corpusNameDesc = new HashSet<>();

    public static void main(String[] args) throws Exception {
        String[] prefixes = args[1].split(",");
        for (int i = 2; i < args.length; i++) {
            try (JarFile jar = new JarFile(args[i])) {
                for (JarEntry e : Collections.list(jar.entries())) {
                    if (e.getName().endsWith(".class")) {
                        try (InputStream is = jar.getInputStream(e)) {
                            scan(is.readAllBytes(), corpus, true);
                        }
                    }
                }
            }
        }
        for (String c : corpus) {
            String[] p = c.split(" ");
            if (p.length >= 3) {
                corpusNameDesc.add(p[0] + " " + p[2] + " " + (p.length > 3 ? p[3] : ""));
            }
        }
        Set<String> mine = new TreeSet<>();
        try (Stream<Path> s = Files.walk(Paths.get(args[0]))) {
            for (Path f : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".class"))::iterator) {
                scan(Files.readAllBytes(f), mine, false);
            }
        }
        int ok = 0, differs = 0, missing = 0;
        for (String r : mine) {
            String[] p = r.split(" ");
            String owner = p.length > 1 ? p[1] : "";
            if (Arrays.stream(prefixes).noneMatch(owner::startsWith)) {
                continue;
            }
            if (corpus.contains(r)) {
                ok++;
                System.out.println("OK            " + r);
            } else if (p.length >= 3 && corpusNameDesc.contains(p[0] + " " + p[2] + " " + (p.length > 3 ? p[3] : ""))) {
                differs++;
                System.out.println("OWNER-DIFFERS " + r);
            } else {
                missing++;
                System.out.println("MISSING       " + r);
            }
        }
        System.out.println("ok=" + ok + " ownerDiffers=" + differs + " missing=" + missing);
    }

    static void scan(byte[] bytes, Set<String> out, boolean declarations) {
        ClassReader reader = new ClassReader(bytes);
        String self = reader.getClassName();
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
                if (declarations) {
                    out.add("F " + self + " " + name + " " + desc);
                }
                return new FieldVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String d, boolean v) {
                        return ann(d, out);
                    }
                };
            }

            @Override
            public AnnotationVisitor visitAnnotation(String d, boolean v) {
                return ann(d, out);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                if (declarations) {
                    out.add("M " + self + " " + name + " " + desc);
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String d, boolean v) {
                        return ann(d, out);
                    }

                    @Override
                    public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
                        out.add("M " + owner + " " + name + " " + desc);
                    }

                    @Override
                    public void visitFieldInsn(int op, String owner, String name, String desc) {
                        out.add("F " + owner + " " + name + " " + desc);
                    }

                    @Override
                    public void visitTypeInsn(int op, String type) {
                        out.add("C " + type);
                    }
                };
            }
        }, 0);
    }

    static AnnotationVisitor ann(String desc, Set<String> out) {
        String type = desc.substring(1, desc.length() - 1);
        out.add("C " + type);
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String name, Object value) {
                out.add("A " + type + " " + name);
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                out.add("A " + type + " " + name);
                return null;
            }

            @Override
            public void visitEnum(String name, String d, String value) {
                out.add("A " + type + " " + name);
            }
        };
    }
}

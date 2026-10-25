import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.stream.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

/**
 * Renames Minecraft members in compiled classes from MCP (stable_39) names to SRG names,
 * which is what ForgeGradle's reobfJar does.
 *
 * Usage: Reobf <mappingsDir> <inClassesDir> <outClassesDir> <hierarchyPath...>
 *   mappingsDir holds joined.srg, methods.csv and fields.csv.
 *   hierarchyPath entries are jars or class dirs used to walk superclasses and interfaces.
 *
 * Prints every Minecraft-owned reference that has no SRG mapping (these should only be
 * Forge-added members), and writes refs.txt listing every remapped reference.
 */
public class Reobf {
    // "owner name desc" (MCP names) -> SRG name
    static final Map<String, String> methods = new HashMap<>();
    // "owner name" (MCP names) -> SRG name
    static final Map<String, String> fields = new HashMap<>();
    // every SRG member that exists: "owner name desc" / "owner name"
    static final Set<String> srgMethods = new HashSet<>();
    static final Set<String> srgFields = new HashSet<>();
    static final Map<String, String[]> supers = new HashMap<>();
    static final Set<String> unmapped = new TreeSet<>();
    static final Set<String> mapped = new TreeSet<>();

    public static void main(String[] args) throws Exception {
        Path mappings = Paths.get(args[0]);
        Map<String, String> methodNames = readCsv(mappings.resolve("methods.csv"));
        Map<String, String> fieldNames = readCsv(mappings.resolve("fields.csv"));
        for (String line : Files.readAllLines(mappings.resolve("joined.srg"))) {
            String[] p = line.split(" ");
            if (p[0].equals("MD:")) {
                String srg = p[3];
                int slash = srg.lastIndexOf('/');
                String owner = srg.substring(0, slash), name = srg.substring(slash + 1), desc = p[4];
                srgMethods.add(owner + " " + name + " " + desc);
                methods.put(owner + " " + methodNames.getOrDefault(name, name) + " " + desc, name);
            } else if (p[0].equals("FD:")) {
                String srg = p[2];
                int slash = srg.lastIndexOf('/');
                String owner = srg.substring(0, slash), name = srg.substring(slash + 1);
                srgFields.add(owner + " " + name);
                fields.put(owner + " " + fieldNames.getOrDefault(name, name), name);
            }
        }
        for (int i = 3; i < args.length; i++) {
            loadHierarchy(Paths.get(args[i]));
        }

        Path in = Paths.get(args[1]), out = Paths.get(args[2]);
        Remapper remapper = new Remapper() {
            @Override
            public String mapMethodName(String owner, String name, String desc) {
                if (name.startsWith("<")) {
                    return name;
                }
                return resolve(owner, name, desc, true);
            }

            @Override
            public String mapFieldName(String owner, String name, String desc) {
                return resolve(owner, name, desc, false);
            }
        };
        List<Path> classes;
        try (Stream<Path> s = Files.walk(in)) {
            classes = s.filter(f -> f.toString().endsWith(".class")).collect(Collectors.toList());
        }
        for (Path f : classes) {
            ClassReader reader = new ClassReader(Files.readAllBytes(f));
            ClassWriter writer = new ClassWriter(0);
            reader.accept(new ClassRemapper(writer, remapper), 0);
            Path target = out.resolve(in.relativize(f));
            Files.createDirectories(target.getParent());
            Files.write(target, writer.toByteArray());
        }
        Files.write(out.resolve("../refs.txt"), mapped);
        System.out.println("Remapped " + classes.size() + " classes, " + mapped.size() + " Minecraft references.");
        for (String u : unmapped) {
            System.out.println("UNMAPPED " + u);
        }
    }

    /** Walks owner's superclasses and interfaces for a Minecraft declaration of this member. */
    static String resolve(String owner, String name, String desc, boolean method) {
        Deque<String> queue = new ArrayDeque<>(Collections.singleton(owner));
        Set<String> seen = new HashSet<>();
        boolean touchedMinecraft = false;
        while (!queue.isEmpty()) {
            String c = queue.poll();
            if (!seen.add(c)) {
                continue;
            }
            if (c.startsWith("net/minecraft/")) {
                touchedMinecraft = true;
                String srg = method ? methods.get(c + " " + name + " " + desc) : fields.get(c + " " + name);
                if (srg != null) {
                    mapped.add((method ? "M " : "F ") + owner + " " + name + (method ? desc : "") + " -> " + c + " " + srg);
                    return srg;
                }
                if (method ? srgMethods.contains(c + " " + name + " " + desc) : srgFields.contains(c + " " + name)) {
                    return name; // already an SRG name
                }
            }
            String[] s = supers.get(c);
            if (s != null) {
                queue.addAll(Arrays.asList(s));
            } else if (!c.startsWith("java/") && !c.startsWith("javax/")) {
                unmapped.add("NO-HIERARCHY " + c + " (while resolving " + owner + " " + name + ")");
            }
        }
        if (touchedMinecraft && !owner.startsWith("java/")) {
            unmapped.add((method ? "M " : "F ") + owner + " " + name + (method ? " " + desc : ""));
        }
        return name;
    }

    static Map<String, String> readCsv(Path p) throws IOException {
        Map<String, String> map = new HashMap<>();
        for (String line : Files.readAllLines(p)) {
            String[] cols = line.split(",", 3);
            if (cols.length >= 2 && (cols[0].startsWith("func_") || cols[0].startsWith("field_"))) {
                map.put(cols[0], cols[1]);
            }
        }
        return map;
    }

    static void loadHierarchy(Path p) throws IOException {
        if (Files.isDirectory(p)) {
            try (Stream<Path> s = Files.walk(p)) {
                for (Path f : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".class"))::iterator) {
                    addClass(Files.readAllBytes(f));
                }
            }
        } else {
            try (JarFile jar = new JarFile(p.toFile())) {
                for (JarEntry e : Collections.list(jar.entries())) {
                    if (e.getName().endsWith(".class")) {
                        try (InputStream is = jar.getInputStream(e)) {
                            addClass(is.readAllBytes());
                        }
                    }
                }
            }
        }
    }

    static void addClass(byte[] bytes) {
        ClassReader r = new ClassReader(bytes);
        List<String> s = new ArrayList<>();
        if (r.getSuperName() != null) {
            s.add(r.getSuperName());
        }
        s.addAll(Arrays.asList(r.getInterfaces()));
        supers.putIfAbsent(r.getClassName(), s.toArray(new String[0]));
    }
}

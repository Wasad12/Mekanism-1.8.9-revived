import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

public class SrgRemapper {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: SrgRemapper <mcp-srg.srg> <input.jar> <output.jar> <class1> [class2] ...");
            System.exit(1);
        }

        String srgFile = args[0];
        String inputJar = args[1];
        String outputJar = args[2];
        Set<String> classesToRemap = new HashSet<>(Arrays.asList(args).subList(3, args.length));

        Map<String, String> fieldMap = new HashMap<>();
        Map<String, String> methodMap = new HashMap<>();

        for (String line : Files.readAllLines(Paths.get(srgFile))) {
            if (line.startsWith("FD: ")) {
                String[] parts = line.substring(4).trim().split("\\s+");
                if (parts.length == 2) {
                    fieldMap.put(parts[0], parts[1]);
                }
            } else if (line.startsWith("MD: ")) {
                String[] parts = line.substring(4).trim().split("\\s+");
                if (parts.length >= 4) {
                    methodMap.put(parts[0] + " " + parts[1], parts[2] + " " + parts[3]);
                }
            }
        }

        System.out.println("Loaded " + fieldMap.size() + " field mappings, " + methodMap.size() + " method mappings");

        Remapper remapper = new Remapper() {
            @Override
            public String mapFieldName(String owner, String name, String desc) {
                String key = owner + "/" + name;
                String srg = fieldMap.get(key);
                if (srg != null) {
                    String result = srg.substring(srg.lastIndexOf('/') + 1);
                    System.out.println("  Field: " + key + " -> " + result);
                    return result;
                }
                return name;
            }

            @Override
            public String mapMethodName(String owner, String name, String desc) {
                String key = owner + "/" + name + " " + desc;
                String srg = methodMap.get(key);
                if (srg != null) {
                    String srgFull = srg.substring(0, srg.lastIndexOf(' '));
                    String result = srgFull.substring(srgFull.lastIndexOf('/') + 1);
                    System.out.println("  Method: " + key + " -> " + result);
                    return result;
                }
                return name;
            }
        };

        JarFile jarFile = new JarFile(inputJar);
        Manifest manifest = jarFile.getManifest();
        JarOutputStream jos;
        if (manifest != null) {
            jos = new JarOutputStream(new FileOutputStream(outputJar), manifest);
        } else {
            jos = new JarOutputStream(new FileOutputStream(outputJar));
        }

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if (entryName.equals("META-INF/MANIFEST.MF")) continue;

            InputStream is = jarFile.getInputStream(entry);
            byte[] bytes = readAll(is);
            is.close();

            if (!entry.isDirectory() && entryName.endsWith(".class") && classesToRemap.contains(entryName)) {
                System.out.println("Remapping: " + entryName);
                ClassReader cr = new ClassReader(bytes);
                ClassWriter cw = new ClassWriter(0);
                ClassRemapper cv = new ClassRemapper(cw, remapper);
                cr.accept(cv, 0);
                bytes = cw.toByteArray();
                System.out.println("  Size: " + bytes.length + " bytes");
            }

            jos.putNextEntry(new JarEntry(entryName));
            jos.write(bytes);
            jos.closeEntry();
        }

        jarFile.close();
        jos.close();

        System.out.println("Done: " + outputJar);
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }
}

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class EncryptStubs {

    private static final String[] EXPECTED_FILES = {
            "F.class",
            "FS.class",
            "FW.class",
            "H.class",
            "P.class",
            "RS.class",
            "Real.class",
            "Real$NumberFormat.class",
            "S.class",
            "SM.class"
    };

    private static final byte[] MAGIC = {(byte) 0xA9, 0x41, 0x50, 0x52, 0x54, 0x4C, 0x01, 0x00};
    private static final byte[] KEY = Base64.getDecoder().decode("wfqN93Dw24zueqoiTM3pEztuvPDyICKsfg1/FWQvGLg=");
    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE_BITS = 128;

    private EncryptStubs() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: java tools/EncryptStubs.java <classes-dir> <output-file>");
        }

        Path sourceDirectory = Paths.get(args[0]);
        Path outputFile = Paths.get(args[1]);
        verifyInputs(sourceDirectory);

        byte[] zip = createZip(sourceDirectory);
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "AES"), new GCMParameterSpec(TAG_SIZE_BITS, iv));
        cipher.updateAAD(MAGIC);
        byte[] encrypted = cipher.doFinal(zip);

        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        archive.write(MAGIC);
        archive.write(iv);
        archive.write(encrypted);

        Path parent = outputFile.toAbsolutePath().getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.write(outputFile, archive.toByteArray());
        System.out.println("Encrypted " + EXPECTED_FILES.length + " stubs into " + outputFile);
    }

    private static void verifyInputs(Path sourceDirectory) throws Exception {
        Set<String> expected = Set.copyOf(Arrays.asList(EXPECTED_FILES));
        Set<String> actual;

        try (Stream<Path> files = Files.list(sourceDirectory)) {
            actual =
                    files
                            .filter(Files::isRegularFile)
                            .map(path -> path.getFileName().toString())
                            .filter(name -> name.endsWith(".class"))
                            .collect(Collectors.toSet());
        }

        if (!actual.equals(expected)) {
            throw new IllegalArgumentException("Expected " + expected + ", found " + actual);
        }
    }

    private static byte[] createZip(Path sourceDirectory) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.setLevel(9);

            for (String name : EXPECTED_FILES) {
                ZipEntry entry = new ZipEntry(name);
                entry.setTime(0);
                zip.putNextEntry(entry);
                zip.write(Files.readAllBytes(sourceDirectory.resolve(name)));
                zip.closeEntry();
            }
        }

        return bytes.toByteArray();
    }
}

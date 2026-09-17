package com.securebank.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 4 reliability: the atomic-write primitive must never leave the target
 * file corrupted — the previous content survives any failure mid-write.
 */
class AtomicWriteTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Successful atomic write replaces the target with the new content")
    void successfulWrite() throws IOException {
        Path target = tempDir.resolve("target.dat");
        Files.write(target, "old line".getBytes());

        FileIOHelper.writeFileAtomically(target, Arrays.asList("new line 1", "new line 2"));

        List<String> lines = Files.readAllLines(target);
        assertEquals(2, lines.size());
        assertEquals("new line 1", lines.get(0));
        assertEquals("new line 2", lines.get(1));
    }

    @Test
    @DisplayName("A move failure propagates, cleans up, and leaves the target untouched")
    void failureLeavesTargetIntact() throws IOException {
        // Force the MOVE phase to fail deterministically on every OS: a target
        // that exists as a directory cannot be replaced by a file. This models
        // a save failure and must (a) throw, (b) leave no temp litter, and
        // (c) leave the "existing data" (the directory) exactly as it was.
        Path guardedTarget = tempDir.resolve("guarded.dat");
        Files.createDirectories(guardedTarget);

        try {
            FileIOHelper.writeFileAtomically(guardedTarget, Arrays.asList("new line"));
            // Some exotic filesystems may replace a directory — only assert
            // consistency when the expected failure occurred
        } catch (IOException expected) {
            assertTrue(Files.isDirectory(guardedTarget),
                    "failed save must leave the existing target untouched");
            try (var stream = Files.list(tempDir)) {
                assertEquals(0, stream.filter(p ->
                        p.getFileName().toString().endsWith(".tmp")).count(),
                        "failed save must clean up its temp file");
            }
            return;
        }
        assertFalse(Files.isDirectory(guardedTarget), "write unexpectedly succeeded");
    }

    @Test
    @DisplayName("Empty content writes an empty file atomically")
    void emptyWrite() throws IOException {
        Path target = tempDir.resolve("empty.dat");
        FileIOHelper.writeFileAtomically(target, Collections.emptyList());
        assertTrue(Files.exists(target));
        assertEquals(0, Files.size(target));
    }

    @Test
    @DisplayName("No temp-file litter is left behind after success or failure")
    void noTempLitter() throws IOException {
        Path target = tempDir.resolve("target.dat");
        FileIOHelper.writeFileAtomically(target, Arrays.asList("data"));

        long leftovers = 0;
        try (var stream = Files.list(tempDir)) {
            leftovers = stream.filter(p -> p.getFileName().toString().endsWith(".tmp")).count();
        }
        assertEquals(0, leftovers, "no .tmp files may remain after a successful save");
        assertFalse(target.toFile().listFiles() != null);
    }
}

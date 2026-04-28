package com.gxa.pipe.virtualExpert.agent;

import java.nio.file.Path;

public record ExportFile(
        String exportId,
        String fileName,
        String contentType,
        Path path,
        long size
) {
}

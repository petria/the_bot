package org.freakz.common.media;

import java.nio.file.Path;

public record MediaStoreReadResult(MediaStoreRecord record, Path file) {
}

package io.github.oumaimacoco.helpdesk.storage;

import java.io.InputStream;

public interface FileStorage {

    String store(InputStream content);

    InputStream open(String key);

    void delete(String key);
}
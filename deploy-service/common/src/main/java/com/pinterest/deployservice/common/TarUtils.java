/**
 * Copyright 2016 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *    
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.deployservice.common;

import com.google.common.io.Closeables;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class TarUtils {

    /**
     * Bundle the given map as a list of files, with key as file name and value as content.
     */
    public static byte[] tar(Map<String, String> data) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        TarArchiveOutputStream taos = new TarArchiveOutputStream(new GZIPOutputStream(os));
        // TAR originally didn't support long file names, so enable the support for it
        taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

        // Get to putting all the files in the compressed output file
        for (Map.Entry<String, String> entry : data.entrySet()) {
            byte[] bytes = entry.getValue().getBytes("UTF8");
            InputStream is = new ByteArrayInputStream(bytes);
            addInputStreamToTar(taos, is, entry.getKey(), bytes.length, TarArchiveEntry.DEFAULT_FILE_MODE);
        }

        taos.close();
        return os.toByteArray();
    }

    static void addInputStreamToTar(TarArchiveOutputStream taos, InputStream is, String path,
        long size, int mode) throws Exception {
        TarArchiveEntry entry = new TarArchiveEntry(path);
        entry.setSize(size);
        entry.setMode(mode);
        try {
            taos.putArchiveEntry(entry);
            IOUtils.copy(is, taos);
        } finally {
            taos.closeArchiveEntry();
            Closeables.closeQuietly(is);
        }
    }

    /**
     * Unbundle the given tar bar as a map, with key as file name and value as content.
     */
    public static Map<String, String> untar(InputStream is) throws Exception {
        TarArchiveInputStream tais = new TarArchiveInputStream(new GZIPInputStream(is));
        Map<String, String> data = new HashMap<String, String>();
        TarArchiveEntry entry;
        while ((entry = tais.getNextTarEntry()) != null) {
            String name = entry.getName();
            byte[] content = new byte[(int) entry.getSize()];
            tais.read(content, 0, content.length);
            data.put(name, new String(content, "UTF8"));
        }
        tais.close();
        return data;
    }
}

package org.example;

import java.util.ArrayList;

public class FileBuilder {

    private final ArrayList<byte[]> m_file_parts = new ArrayList<>();



    public FileBuilder(int _parts_count) {
        for (int i = 0; i < _parts_count; i++) {
            m_file_parts.add(new byte[]{});
        }
    }



    public synchronized void AddFilePart(int _index, byte[] _part) {
        m_file_parts.set(_index, _part);
    }


    public byte[] GetFilePart(int _index) {
        return m_file_parts.get(_index);
    }

}

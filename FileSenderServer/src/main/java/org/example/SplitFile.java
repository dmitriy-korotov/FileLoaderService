package org.example;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;



public class SplitFile {

    private ArrayList<String> m_file_parts;
    private final int m_amount_parts;



    public SplitFile(String _filepath, int _amount_parts) throws IOException, IllegalArgumentException {
        if (_amount_parts < 1) {
            throw new IllegalArgumentException("Expected parts amount greater then zero");
        }

        m_amount_parts = _amount_parts;

        try (FileReader reader = new FileReader(_filepath)) {
            char[] buffer = new char[4096];

            StringBuilder data = new StringBuilder();
            while (reader.read(buffer, 0, 4096) > 0) {
                data.append(buffer);
            }

            FillFileParts(data.toString());
        }
    }



    private void FillFileParts(String _file_data) {
        int part_size = _file_data.length() / m_amount_parts;

        int begin = 0;
        int i = 0;
        for (; i < m_amount_parts - 1; i++) {
            m_file_parts.set(i, _file_data.substring(begin, begin + part_size));
            begin += part_size;
        }

        m_file_parts.set(i, _file_data.substring(begin));
    }



    public int GetAmountParts() {
        return m_amount_parts;
    }


    public String GetPart(int _part_number) {
        return m_file_parts.get(_part_number);
    }
}

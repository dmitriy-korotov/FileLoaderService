package org.example;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;


public class SplitFile {

    private final ArrayList<byte[]> m_file_parts;
    private final int m_amount_parts;



    public SplitFile(String _filepath, int _amount_parts) throws IllegalArgumentException {
        if (_amount_parts < 1) {
            throw new IllegalArgumentException("Expected parts amount greater then zero");
        }

        m_amount_parts = _amount_parts;
        m_file_parts = new ArrayList<>(m_amount_parts);

        try (BufferedInputStream reader = new BufferedInputStream(new FileInputStream(_filepath))) {
            FillFileParts(reader.readAllBytes());
        }
        catch (IOException _ex) {
            System.out.println("=> [ERROR]: " + _ex.getMessage());
        }
    }



    private void FillFileParts(byte[] _file_data) {
        int part_size = _file_data.length / m_amount_parts;

        int begin = 0;
        int i = 0;
        for (; i < m_amount_parts - 1; i++) {
            m_file_parts.add(i, Arrays.copyOfRange(_file_data, begin, begin + part_size));
            begin += part_size;
        }
        m_file_parts.add(i, Arrays.copyOfRange(_file_data, begin, _file_data.length));
    }



    public int GetAmountParts() {
        return m_amount_parts;
    }


    public byte[] GetPart(int _part_number) {
        return m_file_parts.get(_part_number);
    }

}

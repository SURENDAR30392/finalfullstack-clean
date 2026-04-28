package com.fullstack.backend.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ReportExcelService {

    public byte[] generateSimpleReport(int users, int courses, int videos, int enrollments) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Summary");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Metric");
            header.createCell(1).setCellValue("Count");

            String[] labels = {"Users", "Courses", "Videos", "Enrollments"};
            int[] values = {users, courses, videos, enrollments};

            for (int i = 0; i < labels.length; i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(labels[i]);
                row.createCell(1).setCellValue(values[i]);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}

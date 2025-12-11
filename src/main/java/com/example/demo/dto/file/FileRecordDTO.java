package com.example.demo.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileRecordDTO {
    private Long id;
    private String fileName;
    private String url;
    private String fileType;
}

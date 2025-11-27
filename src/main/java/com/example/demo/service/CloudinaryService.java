package com.example.demo.service;

import com.example.demo.model.FileRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CloudinaryService {

    FileRecord uploadFileForPostOrComment(MultipartFile multipartFile, Object relatedEntity) throws IOException;

}

package com.example.edu.sports_predict_live.global.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    @Value("${com.example.upload.path}")
    private String uploadPath;

    @GetMapping("/view")
    public ResponseEntity<Resource> viewFileGet(@RequestParam("fileName") String fileName) {
        log.info("공통 파일 조회 요청: {}", fileName);

        String basePath = uploadPath.endsWith("/") || uploadPath.endsWith("\\") ? uploadPath : uploadPath + File.separator;
        String fullPath = basePath + fileName;

        Resource resource = new FileSystemResource(fullPath);

        if (!resource.exists()) {
            log.warn("요청한 파일이 로컬 디스크에 존재하지 않습니다: {}", fullPath);
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        try {
            String contentType = Files.probeContentType(resource.getFile().toPath());
            headers.add("Content-Type", contentType != null ? contentType : "application/octet-stream");
        } catch (Exception e) {
            log.error("파일 Content-Type 검색 실패", e);
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().headers(headers).body(resource);
    }
}

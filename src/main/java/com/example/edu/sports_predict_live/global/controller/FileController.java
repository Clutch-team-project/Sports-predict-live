package com.example.edu.sports_predict_live.global.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

        Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path resolved = resolveSafely(baseDir, fileName);
        if (resolved == null || !Files.exists(resolved)) {
            // board 하위 폴더로 한 번 더 시도
            Path boardResolved = resolveSafely(baseDir.resolve("board"), fileName);
            if (boardResolved == null || !Files.exists(boardResolved)) {
                log.warn("요청한 파일이 존재하지 않거나 허용 범위 밖입니다: {}", fileName);
                return ResponseEntity.notFound().build();
            }
            resolved = boardResolved;
        }

        HttpHeaders headers = new HttpHeaders();
        try {
            String contentType = Files.probeContentType(resolved);
            headers.add("Content-Type", contentType != null ? contentType : "application/octet-stream");
        } catch (Exception e) {
            log.error("파일 Content-Type 검색 실패", e);
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().headers(headers).body(new FileSystemResource(resolved));
    }

    // 디렉토리 탈출 차단: 정규화 후 baseDir 하위 경로인지 확인
    private Path resolveSafely(Path baseDir, String rawName) {
        if (rawName == null || rawName.isBlank()) return null;
        // null byte 차단
        if (rawName.indexOf('\0') >= 0) return null;
        try {
            Path candidate = baseDir.resolve(rawName).normalize();
            if (!candidate.startsWith(baseDir)) {
                log.warn("디렉토리 탈출 시도 차단: base={}, requested={}", baseDir, rawName);
                return null;
            }
            return candidate;
        } catch (Exception e) {
            log.warn("파일 경로 해석 실패: {}", rawName, e);
            return null;
        }
    }
}

package com.rehab.platform.controller;

import com.rehab.platform.config.SecurityUtils;
import com.rehab.platform.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /** 上传文件（需登录），返回文件路径名 */
    @PostMapping
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        SecurityUtils.currentUser();
        String path = fileService.store(file);
        return Map.of("path", path, "url", "/api/files/" + path);
    }

    /** 下载/预览文件（文件名 UUID 不可猜测，放开访问便于 <img>/<video> 直接引用） */
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        Resource resource = fileService.load(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileService.contentType(filename)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}

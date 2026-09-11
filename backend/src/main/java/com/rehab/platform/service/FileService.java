package com.rehab.platform.service;

import com.rehab.platform.config.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileService {

    private static final Set<String> ALLOWED_EXT = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg",
            "mp4", "mov", "avi", "webm", "m4v", "3gp");

    @Value("${app.upload-dir}")
    private String uploadDir;

    private Path root;

    @PostConstruct
    public void init() throws IOException {
        root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        log.info("文件上传目录: {}", root);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件为空");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot + 1).toLowerCase();
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("不支持的文件类型：" + ext + "（仅支持常见图片与视频格式）");
        }
        String filename = UUID.randomUUID() + "." + ext;
        try {
            Path target = root.resolve(filename).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException("非法文件路径");
            }
            file.transferTo(target);
            return filename;
        } catch (IOException e) {
            throw new BusinessException("文件保存失败：" + e.getMessage());
        }
    }

    public Resource load(String filename) {
        try {
            Path path = root.resolve(filename).normalize();
            if (!path.startsWith(root)) {
                throw new BusinessException(403, "非法文件路径");
            }
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                throw new BusinessException(404, "文件不存在");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new BusinessException(404, "文件不存在");
        }
    }

    public String contentType(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "mp4", "m4v" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "webm" -> "video/webm";
            case "avi" -> "video/x-msvideo";
            case "3gp" -> "video/3gpp";
            default -> "application/octet-stream";
        };
    }
}

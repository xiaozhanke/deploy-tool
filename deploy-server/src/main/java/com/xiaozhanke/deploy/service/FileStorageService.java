package com.xiaozhanke.deploy.service;

import com.xiaozhanke.deploy.config.FileStorageProperties;
import com.xiaozhanke.deploy.exception.BusinessException;
import com.xiaozhanke.deploy.exception.InvalidOperationException;
import com.xiaozhanke.deploy.exception.ResourceNotFoundException;
import com.xiaozhanke.deploy.model.entity.FileRecord;
import com.xiaozhanke.deploy.model.mapper.FileRecordPoVoMapper;
import com.xiaozhanke.deploy.model.request.FileParams;
import com.xiaozhanke.deploy.model.response.PageResult;
import com.xiaozhanke.deploy.model.vo.FileRecordVo;
import com.xiaozhanke.deploy.repository.FileRecordRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件存储服务类
 *
 * @author xiaozhanke
 */
@Slf4j
@Service
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageService {

    private final Path rootDirectory;
    private final FileRecordRepository fileRecordRepository;
    private final FileRecordPoVoMapper fileRecordPoVoMapper;
    private final FileStorageProperties fileStorageProperties;

    public FileStorageService(FileRecordRepository fileRecordRepository,
                              FileRecordPoVoMapper fileRecordPoVoMapper,
                              FileStorageProperties fileStorageProperties) {
        this.fileRecordRepository = fileRecordRepository;
        this.fileRecordPoVoMapper = fileRecordPoVoMapper;
        this.fileStorageProperties = fileStorageProperties;
        // 本地存储根目录绝对路径
        this.rootDirectory = Paths.get(System.getProperty("user.dir"), "files").normalize();
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new BusinessException(String.format("无法创建文件存储根目录: %s", rootDirectory), e);
        }
    }

    /**
     * 保存文件并保存记录
     *
     * @param file       文件
     * @param fileParams 文件记录
     * @return 保存后的文件记录
     */
    @Transactional
    public FileRecordVo storeFile(MultipartFile file, FileParams fileParams) {
        String originalFilename = file.getOriginalFilename();
        String fileName = sanitizeUploadedFileName(originalFilename);
        assertExtensionAllowed(fileName);
        try {
            String relativePath = buildRelativePath(fileParams);
            Path targetDir = getTargetDir(relativePath);
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(fileName).normalize();
            if (!target.startsWith(targetDir)) {
                throw new InvalidOperationException(String.format("非法文件名: %s", originalFilename));
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            // 保存文件记录
            FileRecord fileRecord = new FileRecord();
            BeanUtils.copyProperties(fileParams, fileRecord);
            fileRecord.setFileName(fileName);
            fileRecord.setFileSize(file.getSize());
            fileRecord.setContentType(file.getContentType());
            fileRecord.setRelativePath(relativePath);
            FileRecord save = fileRecordRepository.save(fileRecord);
            return fileRecordPoVoMapper.poToVo(save);
        } catch (IOException e) {
            throw new BusinessException(String.format("保存文件 [%s] 时出错: %s", originalFilename, e.getMessage()), e);
        }
    }

    /**
     * 查询文件记录列表
     *
     * @param params 查询参数
     * @param sort   排序参数
     * @return 文件记录列表
     */
    public List<FileRecordVo> queryList(FileParams params, Sort sort) {
        Specification<FileRecord> specification = buildSpecification(params);
        return fileRecordPoVoMapper.poListToVoList(fileRecordRepository.findAll(specification, sort));
    }

    /**
     * 分页查询文件记录列表
     *
     * @param params   查询参数
     * @param pageable 分页参数
     * @return 分页结果
     */
    public PageResult<FileRecordVo> queryPage(FileParams params, Pageable pageable) {
        Specification<FileRecord> specification = buildSpecification(params);
        Page<FileRecord> page = fileRecordRepository.findAll(specification, pageable);
        List<FileRecordVo> fileRecordList = fileRecordPoVoMapper.poListToVoList(page.getContent());
        return new PageResult<>(fileRecordList, pageable, page.getTotalElements());
    }

    /**
     * 查询文件路径
     *
     * @param params 文件参数
     * @return 文件全路径
     */
    public String queryPath(FileParams params) {
        Specification<FileRecord> specification = buildSpecification(params);
        Sort updateTimeSort = Sort.by(Sort.Order.desc("updateTime"));
        List<FileRecord> fileRecordList = fileRecordRepository.findAll(specification, updateTimeSort);
        if (!fileRecordList.isEmpty()) {
            FileRecord firstRecord = fileRecordList.getFirst();
            Path filePath = getTargetDir(firstRecord.getRelativePath()).resolve(firstRecord.getFileName()).normalize();
            return filePath.toString();
        }
        return null;
    }

    /**
     * 根据文件 Id 查询本地文件路径
     *
     * @param fileId 文件 Id
     * @return 文件全路径
     */
    public String queryPathByFileId(String fileId) {
        FileRecord fileRecord = getFileRecord(fileId);
        Path filePath = getTargetDir(fileRecord.getRelativePath()).resolve(fileRecord.getFileName()).normalize();
        return filePath.toString();
    }

    /**
     * 加载文件
     *
     * @param fileId 文件 Id
     * @return 文件资源
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> loadFile(String fileId) {
        FileRecord fileRecord = getFileRecord(fileId);
        Path filePath = getTargetDir(fileRecord.getRelativePath()).resolve(fileRecord.getFileName()).normalize();
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new BusinessException(HttpStatus.NOT_FOUND.value(), String.format("文件 [%s] 不存在", filePath));
            }
            if (!resource.isReadable()) {
                throw new BusinessException(HttpStatus.FORBIDDEN.value(), String.format("文件 [%s] 不可读", filePath));
            }
            // 内容类型
            String contentType = fileRecord.getContentType();
            // 文件名
            ContentDisposition contentDisposition = ContentDisposition.attachment().filename(fileRecord.getFileName(), StandardCharsets.UTF_8).build();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString()).body(resource);
        } catch (MalformedURLException e) {
            throw new BusinessException(String.format("文件路径 [%s] 格式错误，无法创建资源: %s", filePath, e.getMessage()), e);
        }
    }

    /**
     * 删除文件和记录
     *
     * @param fileId 文件 Id
     */
    @Transactional
    public void deleteFile(String fileId) {
        FileRecord fileRecord = getFileRecord(fileId);
        Path filePath = getTargetDir(fileRecord.getRelativePath()).resolve(fileRecord.getFileName()).normalize();
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new BusinessException(String.format("删除文件 [%s] 时出错: %s", filePath, e.getMessage()), e);
        }
        fileRecord.setDeleted(true);
        fileRecordRepository.save(fileRecord);
    }

    /**
     * 更新文件记录元数据
     *
     * @param fileId     文件 Id
     * @param fileParams 文件参数
     * @return 保存后的文件记录
     */
    @Transactional
    public FileRecordVo updateMetadata(String fileId, FileParams fileParams) {
        FileRecord fileRecord = getFileRecord(fileId);
        fileRecord.setScope(fileParams.getScope());
        fileRecord.setGroupId(fileParams.getGroupId());
        fileRecord.setArtifactId(fileParams.getArtifactId());
        fileRecord.setVersion(fileParams.getVersion());
        fileRecord.setArchitecture(fileParams.getArchitecture());
        fileRecord.setDescription(fileParams.getDescription());
        return fileRecordPoVoMapper.poToVo(fileRecordRepository.save(fileRecord));
    }

    /**
     * 更新文件记录原始文件
     *
     * @param fileId 文件 Id
     * @param file   原始文件
     * @return 保存后的文件记录
     */
    @Transactional
    public FileRecordVo updateRaw(String fileId, MultipartFile file) {
        FileRecord fileRecord = getFileRecord(fileId);
        String originalFilename = file.getOriginalFilename();
        String fileName = sanitizeUploadedFileName(originalFilename);
        assertExtensionAllowed(fileName);
        try {
            String relativePath = fileRecord.getRelativePath();
            Path targetDir = getTargetDir(relativePath);
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(fileName).normalize();
            if (!target.startsWith(targetDir)) {
                throw new InvalidOperationException(String.format("非法文件名: %s", originalFilename));
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            fileRecord.setFileName(fileName);
            fileRecord.setFileSize(file.getSize());
            fileRecord.setContentType(file.getContentType());
        } catch (IOException e) {
            throw new BusinessException(String.format("保存文件 [%s] 时出错: %s", originalFilename, e.getMessage()), e);
        }
        return fileRecordPoVoMapper.poToVo(fileRecordRepository.save(fileRecord));
    }

    /**
     * 获取文件记录
     *
     * @param fileId 文件 Id
     * @return 文件记录
     */
    public FileRecord getFileRecord(String fileId) {
        return fileRecordRepository.findByIdAndDeletedIsFalse(fileId).orElseThrow(() -> new ResourceNotFoundException(String.format("文件记录 [%s] 不存在", fileId)));
    }

    /**
     * 仅作为 FK 占位返回文件代理。
     *
     * <p>用于 DeploymentService 拼装关联实体时只需 fileRecordId 引用而不读取文件元数据的场景；
     * 先校验存在性以便给出 404，再用 {@code getReferenceById} 返回仅持有 ID 的代理。
     *
     * @param fileId 文件记录 Id
     * @return 仅持有 ID 的 FileRecord 代理（必须在事务内访问其他字段才会触发懒加载）
     */
    public FileRecord getFileRecordReference(String fileId) {
        if (!fileRecordRepository.existsByIdAndDeletedIsFalse(fileId)) {
            throw new ResourceNotFoundException(String.format("文件记录 [%s] 不存在", fileId));
        }
        return fileRecordRepository.getReferenceById(fileId);
    }

    /**
     * 构建相对路径
     *
     * @param fileParams 文件参数
     * @return 相对路径
     */
    private String buildRelativePath(FileParams fileParams) {
        StringBuilder pathBuilder = new StringBuilder();
        String relativePath = fileParams.getRelativePath();
        String groupId = fileParams.getGroupId();
        String artifactId = fileParams.getArtifactId();
        String version = fileParams.getVersion();

        if (StringUtils.hasText(relativePath)) {
            pathBuilder.append(relativePath);
            if (!relativePath.endsWith("/")) {
                pathBuilder.append("/");
            }
        } else {
            pathBuilder.append("/");
        }
        if (StringUtils.hasText(groupId)) {
            pathBuilder.append(groupId).append("/");
        }
        if (StringUtils.hasText(artifactId)) {
            pathBuilder.append(artifactId).append("/");
        }
        if (StringUtils.hasText(version)) {
            pathBuilder.append(version).append("/");
        }
        return pathBuilder.toString();
    }

    /**
     * 校验并提取干净的上传文件名。
     *
     * <p>浏览器在不同 OS / 实现下可能把整条本地路径塞进 {@code originalFilename}（典型如老版 IE）。
     * 先 {@link StringUtils#cleanPath} 规范化 {@code ..}，再 {@link StringUtils#getFilename}
     * 剥离任何残留的目录前缀，最后兜底拒绝仍含 {@code ..}、{@code /}、{@code \\} 或控制字符的输入。
     *
     * @param originalFilename 浏览器提交的原始文件名
     * @return 仅包含纯文件名部分的安全名称
     */
    static String sanitizeUploadedFileName(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new InvalidOperationException("文件名不能为空");
        }
        String cleaned = StringUtils.cleanPath(originalFilename);
        String fileName = StringUtils.getFilename(cleaned);
        if (!StringUtils.hasText(fileName)
                || fileName.contains("..")
                || fileName.indexOf('/') >= 0
                || fileName.indexOf('\\') >= 0
                || fileName.indexOf('\0') >= 0
                || fileName.indexOf('\n') >= 0
                || fileName.indexOf('\r') >= 0) {
            throw new InvalidOperationException(String.format("非法文件名: %s", originalFilename));
        }
        return fileName;
    }

    /**
     * 校验文件扩展名是否在白名单内。白名单为空视为放行所有，便于存量环境平滑迁移；
     * 一旦在 {@code application.yml} 显式配置 {@code app.file.allowed-extensions} 即生效。
     *
     * @param fileName 已清洗的纯文件名
     */
    private void assertExtensionAllowed(String fileName) {
        if (!fileStorageProperties.isAllowed(fileName)) {
            throw new InvalidOperationException(String.format(
                    "非法文件类型: [%s]，仅允许 %s",
                    fileName,
                    fileStorageProperties.allowedExtensions()));
        }
    }

    /**
     * 获取目标目录
     *
     * @param relativePath 相对路径
     * @return 目标目录
     */
    private Path getTargetDir(String relativePath) {
        Path targetDir;
        if ((!StringUtils.hasText(relativePath)) || "/".equals(relativePath) || ".".equals(relativePath)) {
            targetDir = rootDirectory;
        } else {
            String normalizedRelative = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;

            targetDir = rootDirectory.resolve(normalizedRelative).normalize();

            if (!targetDir.startsWith(rootDirectory)) {
                throw new InvalidOperationException("非法路径: 禁止访问文件存储根目录之外的位置");
            }
        }
        return targetDir;
    }

    /**
     * 构建复杂查询参数
     *
     * @param params 文件参数
     * @return 查询参数
     */
    private Specification<FileRecord> buildSpecification(FileParams params) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();

            if (StringUtils.hasText(params.getFileName())) {
                predicateList.add(criteriaBuilder.like(root.get("fileName"), "%" + params.getFileName() + "%"));
            }

            if (StringUtils.hasText(params.getRelativePath())) {
                predicateList.add(criteriaBuilder.equal(root.get("relativePath"), params.getRelativePath()));
            }

            if (params.getScope() != null) {
                predicateList.add(criteriaBuilder.equal(root.get("scope"), params.getScope()));
            }

            if (StringUtils.hasText(params.getGroupId())) {
                predicateList.add(criteriaBuilder.equal(root.get("groupId"), params.getGroupId()));
            }

            if (StringUtils.hasText(params.getArtifactId())) {
                predicateList.add(criteriaBuilder.equal(root.get("artifactId"), params.getArtifactId()));
            }

            if (StringUtils.hasText(params.getVersion())) {
                predicateList.add(criteriaBuilder.equal(root.get("version"), params.getVersion()));
            }

            if (params.getArchitecture() != null) {
                predicateList.add(criteriaBuilder.equal(root.get("architecture"), params.getArchitecture()));
            }

            if (StringUtils.hasText(params.getDescription())) {
                predicateList.add(criteriaBuilder.like(root.get("description"), "%" + params.getDescription() + "%"));
            }

            predicateList.add(criteriaBuilder.equal(root.get("deleted"), false));

            return criteriaBuilder.and(predicateList.toArray(new Predicate[0]));
        };
    }

}

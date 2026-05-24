package com.xiaozhanke.deploy.controller;

import com.xiaozhanke.deploy.model.request.FileParams;
import com.xiaozhanke.deploy.model.response.PageResult;
import com.xiaozhanke.deploy.model.vo.FileRecordVo;
import com.xiaozhanke.deploy.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * 文件管理接口
 *
 * @author xiaozhanke
 */
@Tag(name = "files", description = "文件管理接口")
@RestController
@RequestMapping("/files")
public class FileController {
    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 查询所有文件记录列表
     *
     * @param params 查询参数
     * @param sort   排序参数
     * @return 文件记录列表
     */
    @Operation(summary = "查询文件记录列表", description = "查询所有文件记录列表")
    @GetMapping("/list")
    public List<FileRecordVo> queryList(FileParams params, @Parameter(description = "排序参数", example = "{\"sort\": \"updateTime,desc\"}") Sort sort) {
        return fileStorageService.queryList(params, sort);
    }

    /**
     * 分页查询文件记录列表
     *
     * @param params   查询参数
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Operation(summary = "分页查询文件记录列表", description = "分页查询文件记录列表")
    @GetMapping("/page")
    public PageResult<FileRecordVo> queryPage(FileParams params,
                                              @Parameter(description = "分页参数", example = "{\"page\": 0, \"size\": 20, \"sort\": \"updateTime,desc\"}")
                                              @PageableDefault(size = 20, sort = "updateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return fileStorageService.queryPage(params, pageable);
    }

    /**
     * 查询文件路径
     *
     * @param params 文件参数
     * @return 文件路径
     */
    @Operation(summary = "查询文件路径", description = "根据文件参数查询本地文件路径")
    @GetMapping("/path")
    public String queryPath(FileParams params) {
        return fileStorageService.queryPath(params);
    }

    /**
     * 根据文件 Id 查询本地文件路径
     *
     * @param id 文件 Id
     * @return 文件路径
     */
    @Operation(summary = "查询文件路径", description = "根据文件 Id 查询本地文件路径")
    @GetMapping("/{id}/path")
    public String queryPathById(@Parameter(description = "文件 Id", required = true) @PathVariable String id) {
        return fileStorageService.queryPathByFileId(id);
    }

    /**
     * 下载文件
     *
     * @param id 文件 Id
     * @return 文件资源
     */
    @Operation(summary = "下载文件", description = "根据文件 Id 下载文件资源")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadFile(@Parameter(description = "文件 Id", required = true) @PathVariable String id) {
        return fileStorageService.loadFile(id);
    }

    /**
     * 上传文件
     *
     * @param file   文件
     * @param params 文件参数
     * @return 保存后的文件记录
     */
    @Operation(summary = "上传文件", description = "上传文件并保存文件记录")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileRecordVo> uploadFile(@Parameter(description = "文件", required = true) @RequestParam MultipartFile file,
                                                   @ParameterObject @ModelAttribute FileParams params) {
        FileRecordVo storedFile = fileStorageService.storeFile(file, params);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(storedFile.getId()).toUri();
        return ResponseEntity.created(location).body(storedFile);
    }

    /**
     * 删除文件
     *
     * @param id 文件 Id
     */
    @Operation(summary = "删除文件", description = "根据文件 Id 删除文件和记录")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(@Parameter(description = "文件 Id", required = true) @PathVariable String id) {
        fileStorageService.deleteFile(id);
    }

    /**
     * 更新文件记录
     *
     * @param id     文件 Id
     * @param params 文件参数
     * @return 保存后的文件记录
     */
    @Operation(summary = "更新文件记录元数据", description = "更新文件记录元数据")
    @PutMapping("/{id}/metadata")
    public FileRecordVo updateFileMetadata(@Parameter(description = "文件 Id", required = true) @PathVariable String id,
                                           @RequestBody FileParams params) {
        return fileStorageService.updateMetadata(id, params);
    }

    /**
     * 更新文件记录原始文件
     *
     * @param id   文件 Id
     * @param file 原始文件
     * @return 保存后的文件记录
     */
    @Operation(summary = "更新文件记录原始文件", description = "更新文件记录原始文件")
    @PutMapping(value = "/{id}/raw", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileRecordVo updateFileRaw(@Parameter(description = "文件 Id", required = true) @PathVariable String id,
                                      @Parameter(description = "文件", required = true) @RequestParam MultipartFile file) {
        return fileStorageService.updateRaw(id, file);
    }

}

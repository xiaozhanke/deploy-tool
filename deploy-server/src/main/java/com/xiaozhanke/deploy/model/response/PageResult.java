package com.xiaozhanke.deploy.model.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

/**
 * 分页结果
 *
 * @author xiaozhanke
 */
@Data
@Schema(description = "分页结果")
public class PageResult<T> {

    /**
     * 当前页的数据列表
     */
    @Schema(description = "当前页的数据列表")
    private final List<T> content = new ArrayList<>();

    /**
     * 分页信息
     */
    @JsonIgnore
    @Schema(description = "分页信息", hidden = true)
    private final Pageable pageable;

    /**
     * 数据总条数
     */
    @Schema(description = "数据总条数", example = "100")
    private final long totalElements;

    /**
     * 总页数
     */
    @Schema(description = "总页数", example = "10")
    private int totalPages;

    /**
     * 当前页码
     */
    @Schema(description = "当前页码", example = "0")
    private int number;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", example = "20")
    private int size;

    /**
     * 构造方法，初始化分页结果
     *
     * @param content  当前页的数据列表，不能为空
     * @param pageable 分页参数，不能为空
     * @param total    总数据量
     */
    public PageResult(List<T> content, Pageable pageable, long total) {
        Assert.notNull(content, "Content must not be null");
        Assert.notNull(pageable, "Pageable must not be null");

        this.content.addAll(content);
        this.pageable = pageable;
        this.totalElements = pageable.toOptional().filter(it -> !content.isEmpty())
                .filter(it -> it.getOffset() + it.getPageSize() > total)
                .map(it -> it.getOffset() + content.size())
                .orElse(total);
    }

    /**
     * 无分页的构造方法，默认数据列表全部作为内容
     *
     * @param content 当前页的数据列表
     */
    public PageResult(List<T> content) {
        this(content, Pageable.unpaged(), null == content ? 0 : content.size());
    }

    /**
     * 计算总页数
     *
     * @return 总页数，确保至少为 1
     */
    public int getTotalPages() {
        return getSize() == 0 ? 1 : (int) Math.ceil((double) totalElements / (double) getSize());
    }

    /**
     * 获取当前页码
     *
     * @return 当前页码，如果无分页则返回 0
     */
    public int getNumber() {
        return pageable.isPaged() ? pageable.getPageNumber() : 0;
    }

    /**
     * 获取每页大小
     *
     * @return 每页大小，如果无分页则返回内容列表大小
     */
    public int getSize() {
        return pageable.isPaged() ? pageable.getPageSize() : content.size();
    }

}

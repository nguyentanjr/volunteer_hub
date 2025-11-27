package com.example.demo.dto.common;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PaginationResponse<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean hasNext;
    private final boolean hasPrevious;
    private final boolean first;
    private final boolean last;

    public PaginationResponse(Page<T> page) {
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.hasNext = page.hasNext();
        this.hasPrevious = page.hasPrevious();
        this.first = page.isFirst();
        this.last = page.isLast();
    }
}

package com.xinyu.InterviewCoach_v2.dto.response.common;

import java.util.List;

/**
 * 通用分页响应DTO
 */
public class PageResponseDTO<T> {
    private List<T> content;
    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;

    public PageResponseDTO() {}

    public PageResponseDTO(List<T> content, int currentPage, int pageSize, long totalElements, int totalPages) {
        this.content = content;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = currentPage == 1;
        this.last = currentPage == totalPages;
        this.empty = content == null || content.isEmpty();
    }

    // Builder pattern
    public static <T> PageResponseDTO<T> builder() {
        return new PageResponseDTO<>();
    }

    public PageResponseDTO<T> content(List<T> content) {
        this.content = content;
        this.empty = content == null || content.isEmpty();
        return this;
    }

    public PageResponseDTO<T> currentPage(int currentPage) {
        this.currentPage = currentPage;
        this.first = currentPage == 1;
        this.last = currentPage == this.totalPages;
        return this;
    }

    public PageResponseDTO<T> pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public PageResponseDTO<T> totalElements(long totalElements) {
        this.totalElements = totalElements;
        return this;
    }

    public PageResponseDTO<T> totalPages(int totalPages) {
        this.totalPages = totalPages;
        this.last = this.currentPage == totalPages;
        return this;
    }

    // Getters and Setters
    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
        this.empty = content == null || content.isEmpty();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
        this.first = currentPage == 1;
        this.last = currentPage == this.totalPages;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
        this.last = this.currentPage == totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }
}
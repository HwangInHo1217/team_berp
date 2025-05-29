package com.team.berp.order.dto;

import java.util.List;

/**
 * 주문 목록 페이징 결과를 담는 DTO
 */
public class OrderPageDto {
    private List<OrderDto> content; // 현재 페이지의 OrderDto 목록
    private int page;               // 현재 페이지 번호(0-based)
    private int size;               // 페이지당 항목 수
    private long totalElements;     // 전체 주문 수
    private int totalPages;         // 전체 페이지 수

    public OrderPageDto() {}

    public OrderPageDto(List<OrderDto> content,
                        int page,
                        int size,
                        long totalElements,
                        int totalPages) {
        this.content       = content;
        this.page          = page;
        this.size          = size;
        this.totalElements = totalElements;
        this.totalPages    = totalPages;
    }

    // Getter/Setter
    public List<OrderDto> getContent() { return content; }
    public void setContent(List<OrderDto> content) {
        this.content = content;
    }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

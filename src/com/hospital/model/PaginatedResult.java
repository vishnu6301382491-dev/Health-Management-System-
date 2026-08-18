package com.hospital.model;

import java.util.List;

public class PaginatedResult<T> {
    private int totalRecords;
    private int page;
    private int pageSize;
    private int totalPages;
    private List<T> data;

    public PaginatedResult() {}

    public PaginatedResult(int totalRecords, int page, int pageSize, List<T> data) {
        this.totalRecords = totalRecords;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        this.data = data;
    }

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }
}

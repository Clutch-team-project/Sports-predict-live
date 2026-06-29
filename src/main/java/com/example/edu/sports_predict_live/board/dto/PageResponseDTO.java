package com.example.edu.sports_predict_live.board.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Getter
@ToString
public class PageResponseDTO <E>{
    private int page;
    private int size;
    private int total;
    private int totalCount;

    private int start;
    private int end;
    private boolean prev;
    private boolean next;

    private List<E> dtoList;

    @Builder(builderMethodName = "withAll")
    public PageResponseDTO(PageRequestDTO pageRequestDTO, List<E> dtoList, int total, Integer totalCount) {
        if(total <= 0) {
            this.totalCount = (totalCount != null) ? totalCount : 0;
            return;
        }

        this.page = pageRequestDTO.getPage();
        this.size = pageRequestDTO.getSize();
        this.total = total;
        this.totalCount = (totalCount != null) ? totalCount : total;
        this.dtoList = dtoList;

        this.end = (int)(Math.ceil(this.page/10.0)) * 10;
        this.start = this.end - 9;

        int last = (int)(Math.ceil((total / (double)size)));

        if(this.end > last) {
            this.end = last;
        }
        this.prev = this.start > 1;
        this.next = total> this.end * this.size;
    }
}

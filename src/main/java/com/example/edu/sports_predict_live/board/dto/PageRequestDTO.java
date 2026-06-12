package com.example.edu.sports_predict_live.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageRequestDTO {
    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 10;
    private String type;
    private String keyword;
    private String category;
    private String sort;

    public String[] getTypes() {
        if(type == null || type.isEmpty()) {
            return null;
        }
        return type.split("");
    }

    public Pageable getPageable(String...props) {
        return PageRequest.of(this.page - 1, this.size, Sort.by(props).descending());
    }

    private String link;

    public String getLink() {
        if (link == null) {
           StringBuilder builder = new StringBuilder();
           builder.append("page=").append(this.page);
           builder.append("＆size=").append(this.size);

           if(type != null && !type.isEmpty()) {
               builder.append("&type=").append(this.type);
           }
           if (keyword != null && !keyword.isEmpty()) {
               try{
                   builder.append("&keyword=").append(URLEncoder.encode(this.keyword, "UTF-8"));
               } catch (UnsupportedEncodingException e){}
           }
           if(category != null && !category.isEmpty()) {
               builder.append("&category=").append(this.category);
           }
           link = builder.toString();
        }
        return link;
    }
}

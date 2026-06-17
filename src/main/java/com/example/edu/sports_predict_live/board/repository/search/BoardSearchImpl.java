package com.example.edu.sports_predict_live.board.repository.search;

import com.example.edu.sports_predict_live.board.entity.Board;
import com.example.edu.sports_predict_live.board.entity.QBoard;
import com.example.edu.sports_predict_live.board.dto.BoardListAllDTO;
import com.example.edu.sports_predict_live.board.entity.QBoardReply;
import com.example.edu.sports_predict_live.user.entity.QUser;
import com.example.edu.sports_predict_live.user.entity.User;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;
import java.util.stream.Collectors;

public class BoardSearchImpl extends QuerydslRepositorySupport implements BoardSearch {
    public BoardSearchImpl() {
        super(Board.class);
    }

    @Override
    public Page<BoardListAllDTO> searchWithAll(String[] types, String keyword, String category, String sort, Pageable pageable, String boardType) {
        QBoard board = QBoard.board;
        QUser user = QUser.user;
        QBoardReply reply = QBoardReply.boardReply;

        JPQLQuery<Board> query = from(board);
        query.leftJoin(user).on(board.userId.eq(user.userId));
        query.leftJoin(reply).on(reply.board.eq(board));

        // 검색 조건 처리
        if((types != null && types.length > 0) && keyword != null) {
            BooleanBuilder booleanBuilder = new BooleanBuilder();
            for(String typeStr : types) {
                String[] typeArr = typeStr.split("");
                switch (typeStr) {
                    case "t":
                        booleanBuilder.or(board.title.contains(keyword));
                        break;
                    case "c":
                        booleanBuilder.or(board.content.contains(keyword));
                        break;
                    case "w":
                        booleanBuilder.or(user.nickname.contains(keyword)
                                .or(user.loginId.contains(keyword)));
                        break;
                }
            }
            query.where(booleanBuilder);
        }
        // 블라인드 된 글을 검색하지 않을시 list에서 안보임
        if (keyword == null || keyword.trim().isEmpty()) {
            query.where(board.isBlinded.isFalse());
        } else {
            boolean isTitleOrContentSearch = false;

            if (types != null) {
                for (String type : types) {
                    if (type.contains("t") || type.contains("c")) {
                        isTitleOrContentSearch = true;
                        break;
                    }
                }
            }
            if (!isTitleOrContentSearch) {
                query.where(board.isBlinded.isFalse());
            }
        }



        if (category != null && !category.isEmpty()) {
            if ("공지".equals(category)) {
                if (boardType != null && !boardType.isEmpty()) {
                    query.where(board.category.eq("공지")
                            .and(board.boardType.eq(boardType).or(board.boardType.isNull())));
                } else {
                    query.where(board.category.eq("공지").and(board.boardType.isNull()));
                }
            } else {
                query.where(board.isNotice.isFalse());
                query.where(board.category.eq(category));
                if (boardType != null && !boardType.isEmpty()) {
                    query.where(board.boardType.eq(boardType));
                }
            }
        } else {
            query.where(board.isNotice.isFalse());
            if (boardType != null && !boardType.isEmpty()) {
                query.where(board.boardType.eq(boardType));
            }
        }

        if ("공지".equals(category)) {
            if (boardType != null && !boardType.isEmpty()) {
                query.where(board.boardType.eq(boardType).or(board.boardType.isNull()));
            } else {
                query.where(board.boardType.isNull());
            }
        } else {
            if (boardType != null && !boardType.isEmpty()) {
                query.where(board.boardType.eq(boardType));
            }
        }

        if(sort != null) {
            switch (sort) {
                case "view":
                    query.orderBy(board.viewCount.desc(), board.boardId.desc());
                    break;
                case "like":
                    query.orderBy(board.likeCount.desc(), board.boardId.desc());
                    break;
                case "latest":
                default:
                    query.orderBy(board.boardId.desc());
                    break;
            }
        } else {
            query.orderBy(board.boardId.desc());
        }

        query.where(board.boardId.gt(0L));
        query.where(board.deletedAt.isNull());
        query.groupBy(board);

        this.getQuerydsl().applyPagination(pageable, query);

        JPQLQuery<Tuple> tupleQuery = query.select(board, user, reply.countDistinct());

        List<Tuple> tupleList = tupleQuery.fetch();
        long count = query.fetchCount();

        List<BoardListAllDTO> dtoList = tupleList.stream().map(tuple -> {
            Board b = tuple.get(board);
            User u = tuple.get(user);
            Long replyCount = tuple.get(reply.countDistinct());

            List<String> fileNames = b.getImageSet().stream().sorted()
                    .map(img -> img.getUuid() + "_" + img.getFileName())
                    .collect(Collectors.toList());

            return BoardListAllDTO.builder()
                    .boardId(b.getBoardId())
                    .userId(b.getUserId())
                    .loginId(u != null ? u.getLoginId() : null)
                    .nickname(u != null ? u.getNickname() : null)
                    .category(b.getCategory())
                    .title(b.getTitle())
                    .viewCount(b.getViewCount())
                    .likeCount(b.getLikeCount())
                    .isNotice(b.isNotice())
                    .isBlinded(b.isBlinded())
                    .isDeleted(b.getDeletedAt() != null)
                    .createdAt(b.getCreatedAt())
                    .updatedAt(b.getUpdatedAt())
                    .fileNames(fileNames)
                    .replyCount(replyCount != null ? replyCount : 0L)
                    .build();
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, count);
    }
}
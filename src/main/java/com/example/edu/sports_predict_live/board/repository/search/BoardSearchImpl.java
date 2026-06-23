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
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.time.LocalDateTime;
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

        // 카테고리 및 공지사항 분기 처리
        if ("공지".equals(category)) {
            query.where(board.category.eq("공지"));
            if (boardType != null && !boardType.trim().isEmpty()) {
                query.where(board.boardType.eq(boardType)
                        .or(board.boardType.isNull())
                        .or(board.boardType.isEmpty()));
            } else {
                query.where(board.boardType.isNull()
                        .or(board.boardType.isEmpty()));
            }
        }
        else if (category != null && !category.trim().isEmpty()) {
            query.where(board.isNotice.isFalse());
            query.where(board.category.eq(category));
            if (boardType != null && !boardType.trim().isEmpty()) {
                query.where(board.boardType.eq(boardType));
            }
        }
        else {
            query.where(board.isNotice.isFalse());
            if (boardType != null && !boardType.trim().isEmpty()) {
                query.where(board.boardType.eq(boardType));
            }
        }

        // 정렬 조건 처리
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

        if(!"공지".equals(category)) {
            query.where(board.boardId.gt(0L));
        }

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

    @Override
    public List<BoardListAllDTO> findPopularPosts(int limit, String boardType) {
        QBoard board = QBoard.board;
        QUser users = QUser.user;
        QBoardReply boardReply = QBoardReply.boardReply;

        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        JPQLQuery<Board> baseQuery = from(board)
                .leftJoin(users).on(board.userId.eq(users.userId))
                .leftJoin(boardReply).on(boardReply.board.boardId.eq(board.boardId))
                .where(board.createdAt.between(start, end))
                .where(board.deletedAt.isNull())
                .where(board.isBlinded.isFalse())
                .where(board.isNotice.isFalse());

        if (boardType != null && !boardType.trim().isEmpty()) {
            String targetType = boardType.trim().toLowerCase();
            if (targetType.equals("football") || targetType.equals("soccer")) {
                baseQuery.where(board.boardType.equalsIgnoreCase("soccer")
                        .or(board.boardType.equalsIgnoreCase("football")));
            } else {
                baseQuery.where(board.boardType.equalsIgnoreCase(targetType));
            }
        }

        Expression<Long> replyCount = boardReply.replyId.countDistinct();

        JPQLQuery<BoardListAllDTO> finalQuery = baseQuery.select(
                Projections.bean(BoardListAllDTO.class,
                        board.boardId,
                        board.title,
                        users.nickname,
                        board.createdAt,
                        board.viewCount,
                        board.likeCount,
                        board.category,
                        board.boardType,
                        Expressions.as(replyCount, "replyCount")
                )
        );

        finalQuery.groupBy(board.boardId, users.nickname);
        finalQuery.orderBy(board.viewCount.desc(), board.boardId.desc());
        finalQuery.limit(limit);

        return finalQuery.fetch();
    }
}
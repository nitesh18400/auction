package com.work.bidding.impl;
import ch.qos.logback.core.util.StringUtil;
import com.work.bidding.dto.BidRequest;
import com.work.bidding.model.Bid;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

public class BiddingServiceImpl {
    public static Bid getBid(BidRequest bidRequest) {
        return Bid.builder()
                .id(constructBidId(bidRequest))
                .productId(bidRequest.getProductId())
                .bidAmount(bidRequest.getBidAmount())
                .userId(bidRequest.getUserId())
                .bidTime(LocalDateTime.now())
                .bidStatus(Bid.BidStatus.PLACED)
                .build();
    }

    public static String constructBidId(BidRequest bidRequest) {
        // throw exception if productId or userId is null
        if (StringUtil.isNullOrEmpty(bidRequest.getProductId()) || StringUtil.isNullOrEmpty(bidRequest.getUserId())){
            throw new IllegalArgumentException("productId or userId cannot be null or empty");
        }

        return bidRequest.getProductId()+":"+bidRequest.getUserId();
    }
}

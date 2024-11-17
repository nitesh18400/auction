package com.work.scheduler.service;

import com.work.scheduler.client.BiddingClient;
import com.work.scheduler.client.ProductClient;
import com.work.scheduler.dto.ProductDTO;
import com.work.scheduler.dto.WinnerDTO;
import com.work.scheduler.dto.BidDTO;
import com.work.scheduler.producer.KafkaProducer;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@AllArgsConstructor
public class SchedulerService {

    @Autowired
    private final ProductClient productClient;

    @Autowired
    private final BiddingClient biddingClient;

    @Autowired
    private final KafkaProducer kafkaProducer;

    public void processExpiredProducts() {
        List<ProductDTO> expiredProducts = productClient.getExpiredProducts();

        for (ProductDTO product : expiredProducts) {
            processExpiredProduct(product);
        }
    }

    @Async
    public void processExpiredProduct(ProductDTO product) {
        List<BidDTO> bids = biddingClient.getBidsForProduct(product.getId()).getBids();
        WinnerDTO winner = determineWinner(product, bids);
        if (winner != null) {
            kafkaProducer.sendWinnerNotification(winner);
//                kafkaTemplate.sendDefault(winner.toString());

        }
    }

    public WinnerDTO determineWinner(ProductDTO product, List<BidDTO> bids) {
        if (bids == null || bids.isEmpty()) {
            // Handle no bids case, e.g., return null or a specific WinnerDTO indicating no winner
            return null; // Or return a WinnerDTO with appropriate information for no winner
        }

        BidDTO highestBid = bids.stream()
                .max(Comparator.comparing(BidDTO::getBidAmount))
                .orElse(null);

        if (highestBid != null) {
            return new WinnerDTO(highestBid.getUserId(), product.getId(), highestBid.getBidAmount());
        }
        return null;
    }
}

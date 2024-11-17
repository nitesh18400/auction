package com.work.scheduler.service;

import com.work.scheduler.client.BiddingClient;
import com.work.scheduler.client.ProductClient;
import com.work.scheduler.dto.BidDTO;
import com.work.scheduler.dto.BidResponse;
import com.work.scheduler.dto.ProductDTO;
import com.work.scheduler.dto.WinnerDTO;
import com.work.scheduler.producer.KafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class SchedulerServiceTest {

    private SchedulerService schedulerService;

    private ProductClient productClient;
    private BiddingClient biddingClient;
    private KafkaProducer kafkaProducer;

    @BeforeEach
    void setUp() {
        productClient = mock(ProductClient.class);
        biddingClient = mock(BiddingClient.class);
        kafkaProducer = mock(KafkaProducer.class);

        schedulerService = new SchedulerService(productClient, biddingClient, kafkaProducer);
    }

    @Test
    void testProcessExpiredProducts_NoExpiredProducts() {
        when(productClient.getExpiredProducts()).thenReturn(Collections.emptyList());

        schedulerService.processExpiredProducts();

        verify(biddingClient, never()).getBidsForProduct(anyString());
        verify(kafkaProducer, never()).sendWinnerNotification(any());
    }

    @Test
    void testProcessExpiredProducts_NoBids() {
        ProductDTO product = new ProductDTO();
        product.setId("product1");

        when(productClient.getExpiredProducts()).thenReturn(Arrays.asList(product));
        when(biddingClient.getBidsForProduct(product.getId())).thenReturn(new BidResponse(Collections.emptyList()));

        schedulerService.processExpiredProducts();

        verify(biddingClient).getBidsForProduct(product.getId());
        verify(productClient).getExpiredProducts();
        verify(kafkaProducer, never()).sendWinnerNotification(any());
    }

    @Test
    void testProcessExpiredProducts_WinnerDetermined() {
        ProductDTO product = new ProductDTO();
        product.setId("product1");
        LocalDateTime biddingEndTime = LocalDateTime.now().minusDays(1);
        BidDTO bid1 = new BidDTO("bid1", "product1", "user1", 500.0, biddingEndTime);
        BidDTO bid2 = new BidDTO("bid2", "product1", "user2", 600.0, biddingEndTime);

        when(productClient.getExpiredProducts()).thenReturn(Arrays.asList(product));
        when(biddingClient.getBidsForProduct(product.getId())).thenReturn(new BidResponse(Arrays.asList(bid1, bid2)));

        schedulerService.processExpiredProducts();

        ArgumentCaptor<WinnerDTO> winnerCaptor = ArgumentCaptor.forClass(WinnerDTO.class);
        verify(kafkaProducer).sendWinnerNotification(winnerCaptor.capture());
        WinnerDTO capturedWinner = winnerCaptor.getValue();

        assertEquals("user2", capturedWinner.getEmailId());
        assertEquals("product1", capturedWinner.getProductId());
        assertEquals(600.0, capturedWinner.getWinningBidAmount());


        verify(biddingClient).getBidsForProduct(product.getId());
        verify(productClient).getExpiredProducts();
    }

    @Test
    void testDetermineWinner_NoBids() {
        List<BidDTO> bids = Collections.emptyList();
        WinnerDTO winner = schedulerService.determineWinner(new ProductDTO(), bids);
        assertNull(winner);
    }
//
//    @Test
//    void testDetermineWinner_HighestBid() {
//        ProductDTO product = new ProductDTO();
//        BidDTO bid1 = new BidDTO("user1", 500.0);
//        BidDTO bid2 = new BidDTO("user2", 700.0);
//        List<BidDTO> bids = Arrays.asList(bid1, bid2);
//
//        WinnerDTO winner = schedulerService.determineWinner(product, bids);
//
//        assertEquals("user2", winner.getEmailId());
//        assertEquals(product.getId(), winner.getProductId());
//        assertEquals(700.0, winner.getWinningBidAmount());
//    }
}


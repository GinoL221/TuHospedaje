package com.tuhospedaje.service;

import com.tuhospedaje.dto.common.PageResponse;
import com.tuhospedaje.dto.reservation.CreateReservationRequest;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.User;

import java.util.List;

public interface ReservationService {

    ReservationResponse createReservation(User user, CreateReservationRequest request);

    ReservationResponse getReservationById(Long id, User requester);

    ReservationResponse cancelReservation(Long id, User requester);

    List<ReservationResponse> getMyReservations(User user);


    PageResponse<ReservationResponse> getAdminReservations(int page, int size, String sort, String direction, String status, String q);
}

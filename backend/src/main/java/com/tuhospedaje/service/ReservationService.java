package com.tuhospedaje.service;

import com.tuhospedaje.dto.common.PageResponse;
import com.tuhospedaje.dto.reservation.CreateReservationRequest;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import java.util.List;

public interface ReservationService {

    ReservationResponse createReservation(AuthenticatedActor actor, CreateReservationRequest request);

    ReservationResponse getReservationById(Long id, AuthenticatedActor requester);

    ReservationResponse cancelReservation(Long id, AuthenticatedActor requester);

    List<ReservationResponse> getMyReservations(AuthenticatedActor actor);


    PageResponse<ReservationResponse> getAdminReservations(int page, int size, String sort, String direction, String status, String q);
}

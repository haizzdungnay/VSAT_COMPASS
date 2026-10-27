package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Ticket;
import com.example.v_sat_compass.data.model.TicketCreateRequest;
import com.example.v_sat_compass.data.model.TicketUpdateRequest;
import com.example.v_sat_compass.data.model.admin.PageResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TicketApi {
    @POST("tickets")
    Call<ApiResponse<Ticket>> createTicket(@Body TicketCreateRequest request);

    @GET("admin/tickets")
    Call<ApiResponse<PageResponse<Ticket>>> listTickets(
            @Query("status") String status,
            @Query("page") int page,
            @Query("size") int size);

    @PATCH("admin/tickets/{id}")
    Call<ApiResponse<Ticket>> updateTicket(
            @Path("id") Long id,
            @Body TicketUpdateRequest request);
}

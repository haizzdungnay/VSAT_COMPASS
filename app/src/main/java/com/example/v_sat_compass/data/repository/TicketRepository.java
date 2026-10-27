package com.example.v_sat_compass.data.repository;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.TicketApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Ticket;
import com.example.v_sat_compass.data.model.TicketCreateRequest;
import com.example.v_sat_compass.data.model.TicketUpdateRequest;
import com.example.v_sat_compass.data.model.admin.PageResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketRepository {
    private final TicketApi api;

    public TicketRepository() {
        this(ApiClient.getClient().create(TicketApi.class));
    }

    public TicketRepository(TicketApi api) {
        this.api = api;
    }

    public interface TicketCallback {
        void onSuccess(Ticket ticket);
        void onError(String message);
    }

    public interface TicketsCallback {
        void onSuccess(List<Ticket> tickets);
        void onError(String message);
    }

    public void createTicket(Long questionId, String message, TicketCallback callback) {
        api.createTicket(new TicketCreateRequest(questionId, message)).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                    return;
                }
                callback.onError("Khong gui duoc bao loi");
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Loi ket noi");
            }
        });
    }

    public void listTickets(String status, TicketsCallback callback) {
        api.listTickets(status, 0, 100).enqueue(new Callback<ApiResponse<PageResponse<Ticket>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PageResponse<Ticket>>> call,
                                   Response<ApiResponse<PageResponse<Ticket>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    PageResponse<Ticket> page = response.body().getData();
                    callback.onSuccess(page != null && page.getContent() != null
                            ? page.getContent()
                            : new ArrayList<>());
                    return;
                }
                callback.onError("Khong tai duoc ticket");
            }

            @Override
            public void onFailure(Call<ApiResponse<PageResponse<Ticket>>> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Loi ket noi");
            }
        });
    }

    public void updateTicket(Long id, String status, String note, TicketCallback callback) {
        api.updateTicket(id, new TicketUpdateRequest(status, note)).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                    return;
                }
                callback.onError("Khong cap nhat duoc ticket");
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Loi ket noi");
            }
        });
    }
}

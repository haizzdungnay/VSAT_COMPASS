package com.example.v_sat_compass.ui.admin.tickets;

import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.Ticket;
import com.example.v_sat_compass.data.repository.TicketRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class AdminTicketActivity extends AppCompatActivity {
    private final TicketRepository ticketRepository = new TicketRepository();
    private LinearLayout listContainer;
    private TextView tvEmpty;
    private String activeStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildView());
        loadTickets();
    }

    private LinearLayout buildView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.background));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header);

        TextView title = new TextView(this);
        title.setText("Ticket bao loi");
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(title);

        MaterialButton close = new MaterialButton(this);
        close.setText("Dong");
        close.setOnClickListener(v -> finish());
        header.addView(close);

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams filterLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        filterLp.topMargin = dp(12);
        filters.setLayoutParams(filterLp);
        root.addView(filters);
        addFilter(filters, "Tat ca", null);
        addFilter(filters, "Open", "OPEN");
        addFilter(filters, "Đang xử lý", "IN_PROGRESS");
        addFilter(filters, "Closed", "CLOSED");

        tvEmpty = new TextView(this);
        tvEmpty.setText("Chua co ticket");
        tvEmpty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvEmpty.setTextSize(15);
        LinearLayout.LayoutParams emptyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        emptyLp.topMargin = dp(16);
        tvEmpty.setLayoutParams(emptyLp);
        root.addView(tvEmpty);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(0, dp(12), 0, 0);
        scroll.addView(listContainer);
        root.addView(scroll);
        return root;
    }

    private void addFilter(LinearLayout filters, String label, String status) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setTextSize(12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        button.setLayoutParams(lp);
        button.setOnClickListener(v -> {
            activeStatus = status;
            loadTickets();
        });
        filters.addView(button);
    }

    private void loadTickets() {
        ticketRepository.listTickets(activeStatus, new TicketRepository.TicketsCallback() {
            @Override
            public void onSuccess(List<Ticket> tickets) {
                renderTickets(tickets);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AdminTicketActivity.this, message, Toast.LENGTH_SHORT).show();
                renderTickets(java.util.Collections.emptyList());
            }
        });
    }

    private void renderTickets(List<Ticket> tickets) {
        listContainer.removeAllViews();
        boolean empty = tickets == null || tickets.isEmpty();
        tvEmpty.setVisibility(empty ? android.view.View.VISIBLE : android.view.View.GONE);
        if (empty) return;
        for (Ticket ticket : tickets) {
            listContainer.addView(buildTicketCard(ticket));
        }
    }

    private MaterialCardView buildTicketCard(Ticket ticket) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(8));
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(10);
        card.setLayoutParams(cardLp);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(content);

        TextView title = label("#" + ticket.getId() + " · " + safe(ticket.getStatus()), 15, true, R.color.primary);
        content.addView(title);
        content.addView(label("Question: " + (ticket.getQuestionId() != null ? ticket.getQuestionId() : "N/A"), 13, false, R.color.text_secondary));
        content.addView(label(ticket.getMessage(), 14, false, R.color.text_primary));
        if (ticket.getResolverNote() != null && !ticket.getResolverNote().isEmpty()) {
            content.addView(label("Ghi chu: " + ticket.getResolverNote(), 13, false, R.color.text_secondary));
        }

        MaterialButton update = new MaterialButton(this);
        update.setText("Cập nhật");
        update.setOnClickListener(v -> showUpdateDialog(ticket));
        content.addView(update);
        return card;
    }

    private void showUpdateDialog(Ticket ticket) {
        String[] labels = {"OPEN", "IN_PROGRESS", "CLOSED"};
        int checked = 0;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(ticket.getStatus())) checked = i;
        }
        final int[] selected = {checked};
        EditText note = new EditText(this);
        note.setHint("Ghi chu xu ly");
        note.setText(ticket.getResolverNote() != null ? ticket.getResolverNote() : "");
        note.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        note.setMinLines(2);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(16), 0, dp(16), 0);
        form.addView(note);
        new AlertDialog.Builder(this)
                .setTitle("Cập nhật ticket")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> selected[0] = which)
                .setView(form)
                .setPositiveButton("Lưu", (dialog, which) -> updateTicket(ticket, labels[selected[0]], note.getText().toString()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateTicket(Ticket ticket, String status, String note) {
        ticketRepository.updateTicket(ticket.getId(), status, note, new TicketRepository.TicketCallback() {
            @Override
            public void onSuccess(Ticket ticket) {
                Toast.makeText(AdminTicketActivity.this, "Đã cập nhật ticket", Toast.LENGTH_SHORT).show();
                loadTickets();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AdminTicketActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private TextView label(String text, int sp, boolean bold, int color) {
        TextView tv = new TextView(this);
        tv.setText(safe(text));
        tv.setTextSize(sp);
        tv.setTextColor(ContextCompat.getColor(this, color));
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

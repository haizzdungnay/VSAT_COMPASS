package com.example.v_sat_compass.ui.admin.users;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.data.model.UserItem;
import com.example.v_sat_compass.databinding.ItemAdminUserBinding;
import com.example.v_sat_compass.util.UserRoleHelper;

import java.util.ArrayList;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.VH> {

    private List<UserItem> items = new ArrayList<>();
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onClick(UserItem user);
    }

    public void setOnUserClickListener(OnUserClickListener l) { this.listener = l; }

    public void setUsers(List<UserItem> list) {
        this.items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminUserBinding b = ItemAdminUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        UserItem u = items.get(position);
        holder.b.tvFullName.setText(u.getFullName());
        holder.b.tvRoleBadge.setText(u.getRoleDisplayName());

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(40f);
        bg.setColor(UserRoleHelper.getRoleColor(u.getRole()));
        holder.b.tvRoleBadge.setBackground(bg);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(u);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemAdminUserBinding b;
        VH(ItemAdminUserBinding b) { super(b.getRoot()); this.b = b; }
    }
}

package app.test.expensesapp;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {
    private List<Expense> expenseList;
    private OnLongItemClickListener onLongItemClickListener;
    private String currencySymbol;

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    public interface OnLongItemClickListener {
        void onLongItemClick(int position);
    }

    public ExpenseAdapter(List<Expense> expenseList, OnLongItemClickListener onLongItemClickListener, String currencySymbol) {
        this.expenseList = expenseList;
        this.onLongItemClickListener = onLongItemClickListener;
        this.currencySymbol = currencySymbol;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.expense_item, parent, false);
        return new ExpenseViewHolder(view, onLongItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.tvExpenseName.setText(expense.getName());
        holder.tvExpenseValue.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, expense.getValue()));

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    // Change the background color to red
//                    v.setBackgroundColor(Color.RED);

                    // Change the font color of tvExpenseName and tvExpenseValue to white
                    holder.tvExpenseName.setTextColor(Color.RED);
                    holder.tvExpenseValue.setTextColor(Color.RED);

                    // Post a delayed Runnable to revert the color and perform deletion
                    v.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Revert the background color to its original state
                            v.setBackgroundResource(0);

                            // Revert the font color of tvExpenseName and tvExpenseValue to their original state
                            holder.tvExpenseName.setTextColor(Color.BLACK);
                            holder.tvExpenseValue.setTextColor(Color.BLACK);

                            // Perform the deletion
                            if (onLongItemClickListener != null) {
                                onLongItemClickListener.onLongItemClick(adapterPosition);
                            }
                        }
                    }, 300); // You can adjust the delay (in milliseconds) according to your preference
                }
                return true;
            }
        });
    }




    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvExpenseName;
        TextView tvExpenseValue;

        public ExpenseViewHolder(@NonNull View itemView, final OnLongItemClickListener onLongItemClickListener) {
            super(itemView);
            tvExpenseName = itemView.findViewById(R.id.tv_expense_name);
            tvExpenseValue = itemView.findViewById(R.id.tv_expense_value);

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (onLongItemClickListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            onLongItemClickListener.onLongItemClick(position);
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
    }
}

package app.test.expensesapp;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Configuration;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import api.ExchangeRatesApi;
import api.RetrofitInstance;
import model.ExchangeRates;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements ExpenseAdapter.OnLongItemClickListener {
    private List<Expense> expenseList = new ArrayList<>();
    private ExpenseAdapter expenseAdapter;
    private TextView tvTotalSpent;
    private String baseCurrency = "USD";

    private String selectedCurrency = "USD";
    private ExchangeRates exchangeRates;
    private Spinner currencySpinner;

    private AppDatabase appDatabase;

    private List<Expense> originalExpenseList = new ArrayList<>();



    private void populateDummyData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Expense> expenses = appDatabase.expenseDao().getAllExpenses();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        expenseList.addAll(expenses);
                        expenseAdapter.notifyDataSetChanged();
                        updateTotalSpent(getCurrencySymbol(selectedCurrency));
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appDatabase = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "expense_database").build();

        tvTotalSpent = findViewById(R.id.tv_total_spent);

        RecyclerView rvExpenseList = findViewById(R.id.rv_expense_list);
        expenseAdapter = new ExpenseAdapter(expenseList, this, getCurrencySymbol(selectedCurrency));
        rvExpenseList.setLayoutManager(new LinearLayoutManager(this));
        //currency spinner
        fetchExchangeRates(selectedCurrency);
        currencySpinner = findViewById(R.id.currency_spinner);

        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String currency = parent.getItemAtPosition(position).toString();
                // Perform currency conversion and update UI
                convertExpensesToCurrency(currency);
                updateTotalSpent(getCurrencySymbol(selectedCurrency)); // Add this line here
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        updateTotalSpent(getCurrencySymbol(selectedCurrency));

        rvExpenseList.setAdapter(expenseAdapter);

        Button btnAddExpense = findViewById(R.id.btn_add_expense);
        btnAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddExpenseDialog();
            }
        });
        loadExpensesFromDatabase();

//        populateDummyData();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem darkModeItem = menu.findItem(R.id.action_dark_mode);
        SwitchCompat darkModeSwitch = (SwitchCompat) darkModeItem.getActionView();

        // Set the initial state of the SwitchCompat based on the current theme
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        darkModeSwitch.setChecked(currentNightMode == Configuration.UI_MODE_NIGHT_YES);

        darkModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            }
        });

        return true;
    }

    @Override
    public void onLongItemClick(int position) {
        Expense expense = expenseList.get(position);
        new Thread(new Runnable() {
            @Override
            public void run() {
                appDatabase.expenseDao().delete(expense);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        expenseList.remove(position);
                        expenseAdapter.notifyItemRemoved(position);
                        updateTotalSpent(getCurrencySymbol(selectedCurrency));
                    }
                });
            }
        }).start();
    }

    private void fetchExchangeRates(String baseCurrency) {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.apilayer.com/exchangerates_data/latest").newBuilder();
        urlBuilder.addQueryParameter("base", baseCurrency);
        urlBuilder.addQueryParameter("apikey", "SBh9Ospyefvmn0HINv10i2X0Pj51OihE");

        Request request = new Request.Builder()
                .url(urlBuilder.build().toString())
                .get()
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Failed to fetch exchange rates", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseJson = null;
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        responseJson = responseBody.string();
                    }
                    if (responseJson != null) {
                        Gson gson = new Gson();
                        exchangeRates = gson.fromJson(responseJson, ExchangeRates.class);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setupCurrencySpinner();
                            }
                        });
                    }
                }
            }
        });
    }


    private void loadExpensesFromDatabase() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final List<Expense> loadedExpenses = appDatabase.expenseDao().getAllExpenses();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        expenseList.clear();
                        expenseList.addAll(loadedExpenses);
                        expenseAdapter.notifyDataSetChanged();
                        updateTotalSpent(getCurrencySymbol(selectedCurrency));
                    }
                });
            }
        });
    }


    private void setupCurrencySpinner() {
        List<String> currencies = new ArrayList<>(exchangeRates.getRates().keySet());

        // Add the 10 most used currencies
        List<String> mostUsedCurrencies = Arrays.asList("USD", "EUR", "JPY", "GBP", "AUD", "CAD", "CHF", "CNY", "SEK", "NZD");
        currencies.retainAll(mostUsedCurrencies);

        // Sort the most used currencies list
        Collections.sort(currencies);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(adapter);

        // Set the default selection to USD
        int usdPosition = adapter.getPosition("USD");
        currencySpinner.setSelection(usdPosition);

        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String currency = parent.getItemAtPosition(position).toString();
                // Perform currency conversion and update UI
                convertExpensesToCurrency(currency);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private String getCurrencySymbol(String currencyCode) {
        switch (currencyCode) {
            case "AUD":
                return "A$";
            case "CAD":
                return "C$";
            case "CHF":
                return "CHF";
            case "CNY":
                return "¥";
            case "EUR":
                return "€";
            case "GBP":
                return "£";
            case "JPY":
                return "¥";
            case "NZD":
                return "NZ$";
            case "SEK":
                return "kr";
            case "USD":
                return "$";
            default:
                return "";
        }
    }

    private void convertExpensesToCurrency(String targetCurrency) {
        if (exchangeRates == null || targetCurrency.equals(selectedCurrency)) {
            return;
        }

        double baseToTargetRate = exchangeRates.getRates().get(targetCurrency);
        double selectedToBaseRate = 1 / exchangeRates.getRates().get(selectedCurrency);

        for (Expense expense : expenseList) {
            double baseValue = expense.getValue() * selectedToBaseRate;
            double convertedValue = baseValue * baseToTargetRate;
            expense.setValue(convertedValue);
        }
        expenseAdapter.setCurrencySymbol(getCurrencySymbol(targetCurrency));
        expenseAdapter.notifyDataSetChanged();
        updateTotalSpent(getCurrencySymbol(targetCurrency)); // Change this line
        selectedCurrency = targetCurrency;
    }

    private void showAddExpenseDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.add_expense_dialog, null);
        final EditText etExpenseName = dialogView.findViewById(R.id.et_expense_name);
        final EditText etExpenseValue = dialogView.findViewById(R.id.et_expense_value);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setTitle("Add Expense")
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = etExpenseName.getText().toString().trim();
                        String valueString = etExpenseValue.getText().toString().trim();

                        if (name.isEmpty() || valueString.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                        } else {
                            double value = Double.parseDouble(valueString);
                            addExpense(new Expense(name, value));
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void addExpense(Expense expense) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                appDatabase.expenseDao().insert(expense);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        expenseList.add(expense);
//                        originalExpenseList.add(new Expense(expense.getName(), expense.getValue())); // Add the expense to the originalExpenseList
                        expenseAdapter.notifyDataSetChanged();
                        updateTotalSpent(getCurrencySymbol(selectedCurrency));
                    }
                });
            }
        }).start();
    }


    private void updateTotalSpent(String currencySymbol) {
        double totalSpent = 0;
        for (Expense expense : expenseList) {
            totalSpent += expense.getValue();
        }
        tvTotalSpent.setText(String.format(Locale.getDefault(), "Total spent: %s%.2f", currencySymbol, totalSpent));
    }

}

package app.test.expensesapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expense_table")
public class Expense {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private double value;

    public Expense(String name, double value) {
        this.name = name;
        this.value = value;
    }

    // Add a setter for the id
    public void setId(int id) {
        this.id = id;
    }

    // Add getter methods
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    // Add setter methods (except for id)
    public void setName(String name) {
        this.name = name;
    }

    public void setValue(double value) {
        this.value = value;
    }

}
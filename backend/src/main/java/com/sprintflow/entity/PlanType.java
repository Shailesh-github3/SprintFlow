package com.sprintflow.entity;

public enum PlanType {

    FREE(0.0),
    MONTHLY(9.99),
    YEARLY(99.99);

    private final double price;

    PlanType(double price) {
        this.price = price;
    }

    public double getPrice() {
        return price;
    }
}

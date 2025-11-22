package com.salesdemo.repository;

import com.salesdemo.model.Sales;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class SalesRepo {

    private List<Sales> salesList = new ArrayList<>();

    // Get all
    public List<Sales> findAll() {
        return salesList;
    }

    // Add or Update (same function "save")
    public void save(Sales sale) {

        // Check if product already exists
        for (int i = 0; i < salesList.size(); i++) {
            Sales existing = salesList.get(i);

            if (existing.getProduct_id() == sale.getProduct_id()) {
                // UPDATE existing record
                salesList.set(i, sale);
                return;
            }
        }

        // ADD new record
        salesList.add(sale);
    }

    // Delete by product_id
    public void deleteById(int id) {
        salesList.removeIf(sale -> sale.getProduct_id() == id);
    }
}


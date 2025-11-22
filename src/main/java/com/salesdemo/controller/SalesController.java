package com.salesdemo.controller;

import com.salesdemo.model.Sales;
import com.salesdemo.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SalesController {

    @Autowired
    SaleService service;

    @GetMapping("/employee")
    public List<Sales> getSales(){
        return service.getAllSales();
    }

    @PostMapping("/employee")
    public boolean addSales(@RequestBody Sales data){
        return service.insertData(data);
    }

    @PutMapping("/employee")
    public boolean updateSales(@RequestBody Sales data){
        return service.updateData(data);
    }

    @DeleteMapping("/employee/{id}")
    public boolean deleteSaleById(@PathVariable int id){
        return service.deleteSale(id);
    }
}


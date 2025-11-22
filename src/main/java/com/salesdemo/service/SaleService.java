package com.salesdemo.service;

import com.salesdemo.model.Sales;
import com.salesdemo.repository.SalesRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SaleService {

    @Autowired
    SalesRepo repo;

    public List<Sales> getAllSales(){
        return repo.findAll();
    }

    public boolean insertData(Sales sale){
        try{
            repo.save(sale);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public boolean updateData(Sales sale){
        try{
            repo.save(sale);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteSale(int id){
        try{
            repo.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void hello(){
        System.out.println("Hello World! ");
    }


}


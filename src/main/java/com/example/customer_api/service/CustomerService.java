package com.example.customer_api.service;

import com.example.customer_api.dto.CustomerRequestDTO;
import com.example.customer_api.dto.CustomerResponseDTO;
import com.example.customer_api.dto.CustomerUpdateDTO; 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.customer_api.entity.CustomerStatus;

import java.util.List;

public interface CustomerService {
    
    Page<CustomerResponseDTO> getAllCustomers(Pageable pageable);
    
    CustomerResponseDTO getCustomerById(Long id);
    CustomerResponseDTO createCustomer(CustomerRequestDTO requestDTO);
    CustomerResponseDTO updateCustomer(Long id, CustomerRequestDTO requestDTO);
    CustomerResponseDTO partialUpdateCustomer(Long id, CustomerUpdateDTO updateDTO);
    
    void deleteCustomer(Long id);

    List<CustomerResponseDTO> searchCustomers(String keyword);
    List<CustomerResponseDTO> getCustomersByStatus(CustomerStatus status);
}
package com.dcits.bank.demo.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Customer {
    private Long customerId;
    private String customerName;
    private String type;
    private String idType;
    private String idNumber;
    private String phone;
    private String address;
    private LocalDate dateOfBirth;
    private String gender;
    private Integer age;
    private String branch;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updateTime;
}

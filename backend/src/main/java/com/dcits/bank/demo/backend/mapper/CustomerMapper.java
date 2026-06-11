package com.dcits.bank.demo.backend.mapper;

import com.dcits.bank.demo.backend.entity.Customer;
import org.apache.ibatis.annotations.*;

@Mapper
public interface CustomerMapper {

    @Select("SELECT * FROM customer WHERE id_type = #{idType} AND id_number = #{idNumber}")
    Customer selectByIdTypeAndIdNumber(@Param("idType") String idType, @Param("idNumber") String idNumber);

    @Select("SELECT * FROM customer WHERE customer_id = #{customerId}")
    Customer selectById(@Param("customerId") Long customerId);

    @Insert("INSERT INTO customer (customer_name, type, id_type, id_number, phone, address, date_of_birth, gender, age, branch, status) " +
            "VALUES (#{customerName}, #{type}, #{idType}, #{idNumber}, #{phone}, #{address}, #{dateOfBirth}, #{gender}, #{age}, #{branch}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "customerId")
    int insert(Customer customer);

    @Update("UPDATE customer SET phone = #{phone}, address = #{address} WHERE customer_id = #{customerId}")
    int updateContact(Customer customer);
}

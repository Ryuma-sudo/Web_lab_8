# Customer API

## Student Information
- **Name:** Nguyễn Quang Trực
- **Student ID:** ITCSIU23041
- **Class:** Group 2

## API Endpoints

### Base URL
`http://localhost:8080/api/customers`

### Endpoints Implemented
- [x] GET /api/customers - Get all customers
- [x] GET /api/customers/{id} - Get by ID
- [x] POST /api/customers - Create customer
- [x] PUT /api/customers/{id} - Update customer
- [x] DELETE /api/customers/{id} - Delete customer
- [x] GET /api/customers/search?keyword={keyword} - Search
- [x] GET /api/customers/status/{status} - Filter by status
- [ ] Pagination and sorting
- [ ] PATCH for partial update
- [ ] Bonus features

## How to Run
1. Create database: `customer_management`
2. Update `application.properties` with your MySQL credentials
3. Run: `mvn spring-boot:run`
4. Test: Open Thunder Client or Postman
5. Import collection: `Customer_API.postman_collection.json`

## Testing
All endpoints tested with Thunder Client.
See `screenshots/` folder for test results.

## Features Implemented
- DTO pattern for request/response
- Validation with @Valid
- Exception handling with @RestControllerAdvice
- Custom exceptions (404, 409)
- Proper HTTP status codes
- Search and filter
- Pagination
- Sorting

## Time Spent
Approximately [X] hours

## Project Structure
```
customer-api
├── pom.xml                                     # Dependency management (Spring Web, Data JPA, Validation)
└── src
    └── main
        ├── java
        │   └── com
        │       └── example
        │           └── customerapi
        │               ├── CustomerApiApplication.java
        │               ├── controller          # Presentation Layer (REST Endpoints)
        │               │   └── CustomerRestController.java
        │               ├── dto                 # Data Transfer Objects (Validation & Data Hiding)
        │               │   ├── CustomerRequestDTO.java
        │               │   ├── CustomerResponseDTO.java
        │               │   └── ErrorResponseDTO.java
        │               ├── entity              # Persistence Layer (JPA Entities)
        │               │   ├── Customer.java
        │               │   └── CustomerStatus.java
        │               ├── exception           # Global Error Handling
        │               │   ├── DuplicateResourceException.java
        │               │   ├── GlobalExceptionHandler.java
        │               │   └── ResourceNotFoundException.java
        │               ├── repository          # Data Access Layer (DAO)
        │               │   └── CustomerRepository.java
        │               └── service             # Business Logic Layer
        │                   ├── CustomerService.java
        │                   └── CustomerServiceImpl.java
        └── resources
            └── application.properties          # Database & Server Configuration
```
-----

### 1\. Exercise 1: Project Configuration & Entity Modeling

**Objective:** Establish the Spring Boot environment, configure MySQL connectivity, and define the persistent domain model.

**Implementation Details:**

  * **Configuration:** Configured `application.properties` for MySQL connectivity with Hibernate DDL set to `update`.
  * **Entity Mapping:** Implemented the `Customer` class using JPA annotations. Lifecycle callbacks (`@PrePersist`, `@PreUpdate`) were utilized to automate timestamp management, ensuring data consistency without manual intervention.

**Source Code: `src/main/java/com/example/customerapi/entity/Customer.java`**

```java
package com.example.customerapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_code", unique = true, nullable = false, length = 20)
    private String customerCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE'")
    private CustomerStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Lifecycle hooks for timestamp management
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = CustomerStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Constructors, Getters, and Setters omitted for brevity
}
```

-----

### 2\. Exercise 2: DTO Layer & Validation Strategy

**Objective:** Implement the Data Transfer Object (DTO) pattern to decouple the internal persistence layer from the external API interface and enforce input validation.

**Implementation Details:**

  * **Request DTO:** Created `CustomerRequestDTO` using Jakarta Validation annotations (`@NotBlank`, `@Pattern`, `@Size`) to sanitize inputs before they reach the service layer.
  * **Response DTO:** Designed `CustomerResponseDTO` to expose only safe, relevant data, preventing the accidental leakage of internal entity states.
  * **Error DTO:** structured `ErrorResponseDTO` to provide consistent error messaging (timestamp, status, path, details) across the API.

**Source Code: `src/main/java/com/example/customerapi/dto/CustomerRequestDTO.java`**

```java
package com.example.customerapi.dto;

import jakarta.validation.constraints.*;

public class CustomerRequestDTO {

    @NotBlank(message = "Customer code is required")
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^C\\d{3,}$", message = "Format must be 'C' followed by at least 3 digits")
    private String customerCode;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{10,20}$", message = "Invalid phone number format")
    private String phone;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    private String status;

    // Getters and Setters omitted
}
```

-----

### 3\. Exercise 3: Service Layer & Business Logic

**Objective:** Encapsulate business rules and Entity-DTO conversion logic within a transactional service layer.

**Implementation Details:**

  * **Repository:** Extended `JpaRepository` to leverage standard CRUD operations and defined custom query methods for existence checks (`existsByEmail`, `existsByCustomerCode`).
  * **Service Implementation:** Implemented `CustomerServiceImpl` with `@Transactional`. The logic includes:
    1.  **Duplicate Checks:** Throwing `DuplicateResourceException` if unique constraints (email/code) are violated.
    2.  **Mapping:** Helper methods convert between DTOs and Entities manually to maintain control over data transformation.
    3.  **Exception Handling:** Throwing `ResourceNotFoundException` when querying non-existent IDs.

**Source Code: `src/main/java/com/example/customerapi/service/CustomerServiceImpl.java`**

```java
@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public CustomerResponseDTO createCustomer(CustomerRequestDTO requestDTO) {
        // Validate uniqueness
        if (customerRepository.existsByCustomerCode(requestDTO.getCustomerCode())) {
            throw new DuplicateResourceException("Customer code already exists");
        }
        if (customerRepository.existsByEmail(requestDTO.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        // Convert, Save, and Return
        Customer customer = convertToEntity(requestDTO);
        Customer savedCustomer = customerRepository.save(customer);
        return convertToResponseDTO(savedCustomer);
    }
    
    // Additional CRUD methods (update, delete, get) implemented similarly...
}
```

-----

### 4. API Testing & Verification

The following screenshots demonstrate the successful validation of the core REST API endpoints using Thunder Client. These tests confirm the implementation of the Controller-Service-Repository layers and the global exception handling strategy required for Part A.

#### 4.1. Core CRUD Operations

**Figure 1: Retrieve All Customers (GET)**
* **Endpoint:** `GET /api/customers`
* **Result:** **200 OK**. The response returns a JSON array of `CustomerResponseDTO` objects. This confirms the repository successfully fetches entities and the service layer correctly maps them to DTOs, excluding sensitive internal fields.

<img width="2978" height="1138" alt="image" src="https://github.com/user-attachments/assets/7e853aba-52aa-4e73-99d8-382cba252533" />

**Figure 2: Retrieve Customer by ID (GET)**
* **Endpoint:** `GET /api/customers/{id}`
* **Result:** **200 OK**. The API successfully locates a specific resource. The response body contains the details of the requested customer.

<img width="2975" height="545" alt="image" src="https://github.com/user-attachments/assets/bd7668a1-89c9-4ac6-b76a-fa610e7fb64e" />

**Figure 3: Create New Customer (POST)**
* **Endpoint:** `POST /api/customers`
* **Payload:** Valid `CustomerRequestDTO` JSON.
* **Result:** **201 Created**. The server successfully persisted the new entity. The response includes the generated `id` and `createdAt` timestamps, confirming the `@PrePersist` lifecycle hook.

<img width="2970" height="623" alt="image" src="https://github.com/user-attachments/assets/733409d6-f0e8-41e5-8513-faa6a2d4b937" />

**Figure 4: Update Existing Customer (PUT)**
* **Endpoint:** `PUT /api/customers/{id}`
* **Result:** **200 OK**. The request successfully modified the customer details. The response reflects the updated state of the resource.

<img width="2955" height="582" alt="image" src="https://github.com/user-attachments/assets/70335b75-8de2-4acb-9f1a-ba181a9b134e" />

**Figure 5: Delete Customer (DELETE)**
* **Endpoint:** `DELETE /api/customers/{id}`
* **Result:** **200 OK**. The resource was successfully removed from the database. Subsequent GET requests for this ID will result in a 404 error.

<img width="2974" height="302" alt="image" src="https://github.com/user-attachments/assets/7807e3dd-6f5d-40c7-8dab-76362d122029" />

#### 4.2. Search Capabilities

**Figure 6: Search Customer (GET)**

* **Endpoint:** `GET /api/customers/search?keyword={value}`
* **Result:** **200 OK**. The query returned a list of customers matching the provided keyword (applied to Name, Email, or Customer Code). This validates the custom JPQL queries defined in the CustomerRepository.

<img width="2977" height="896" alt="image" src="https://github.com/user-attachments/assets/593e5d0b-8e13-4622-ab29-149babcb16b6" />

#### 4.3. Exception Handling & Validation

**Figure 7: Input Validation Failure (400)**
* **Scenario:** Sending a payload with invalid data (e.g., invalid email format or blank name).
* **Result:** **400 Bad Request**. The `MethodArgumentNotValidException` was caught by the global handler. The response body details specific field errors, confirming that `@Valid` constraints are active.

<img width="2955" height="622" alt="image" src="https://github.com/user-attachments/assets/8ffde19a-7553-4e1a-bfa1-a7654480c479" />

**Figure 8: Resource Not Found (404)**
* **Scenario:** Requesting an ID that does not exist in the database (e.g., `GET /api/customers/999`).
* **Result:** **404 Not Found**. The `ResourceNotFoundException` was triggered, returning a structured error DTO rather than a generic server trace.

<img width="2984" height="519" alt="image" src="https://github.com/user-attachments/assets/76c16641-eed7-447c-839e-1e9c11f0f692" />

**Figure 9: Duplicate Resource Conflict (409)**
* **Scenario:** Attempting to create a customer with an email or code that already exists.
* **Result:** **409 Conflict**. The service layer detected the unique constraint violation and threw `DuplicateResourceException`.

<img width="2974" height="533" alt="image" src="https://github.com/user-attachments/assets/f9f00614-3374-4e17-a65a-7b13b6118e22" />

-----


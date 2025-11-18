package org.example;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@jakarta.persistence.Table(name = "cafe_tables")  // Fully qualified annotation
public class Table {

    @Id
    @Column(nullable = false)
    private Long id;

    @Min(value = 1, message = "Minimum table size is 1")
    @Max(value = 9, message = "Maximum table size is 9")
    @Column(nullable = false)
    private Long tableSize;

    // Default constructor (needed by Spring)
    public Table() {
    }

    public Table(Long id, Long tableSize) {
        this.id = id;
        this.tableSize = tableSize;
    }

    /*
     * Table configuration:
     * - Tables 1, 2, 3: up to 2 guests
     * - Tables 4, 5, 6, 7: up to 6 guests
     * - Tables 8, 9: up to 9 guests
     */

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTableSize() {
        return tableSize;
    }

    public void setTableSize(Long tableSize) {
        this.tableSize = tableSize;
    }
}

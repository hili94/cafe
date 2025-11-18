package org.example;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tables")
public class TableRestController {

    private final TableRepository tableRepository;

    public TableRestController(TableRepository tableRepository) {
        this.tableRepository = tableRepository;

    }

    /**
     * Get all configured tables (Inventory Catalog)
     * Returns the list of tables and their capacities.
     */
    @GetMapping
    public ResponseEntity<List<Table>> getAllTables() {
        return ResponseEntity.ok(tableRepository.findAll());
    }
}

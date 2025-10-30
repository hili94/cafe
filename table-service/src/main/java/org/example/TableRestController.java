package org.example;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tables")
public class TableRestController {

    private final TableRepository tableRepository;

    public TableRestController(TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }
}

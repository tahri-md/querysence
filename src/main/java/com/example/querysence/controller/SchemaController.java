package com.example.querysence.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.querysence.model.dto.SchemaDefinitionDto;
import com.example.querysence.model.dto.TableDefinitionDto;
import com.example.querysence.model.dto.TableDefinitionRequest;
import com.example.querysence.service.SchemaManagementService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/schemas")
public class SchemaController {
    @Autowired
    SchemaManagementService smService;

    @GetMapping("/{id}")
    public ResponseEntity<SchemaDefinitionDto> getSchema(@PathVariable long id) {
        return ResponseEntity.ok(smService.getSchema(id));
    }
    @PostMapping("/{id}/tables")
    public ResponseEntity<TableDefinitionDto> createSchemaTable(@PathVariable long id,@RequestBody TableDefinitionRequest request ) {
        
        return ResponseEntity.ok(smService.createSchemaTable(request));
    }
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteSchema(@PathVariable long id) {
        smService.deleteSchema(id);
        return ResponseEntity.noContent().build();
    }
    
    
}

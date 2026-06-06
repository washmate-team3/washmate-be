package swp391.carwash.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.ApiResponse;
import swp391.carwash.dto.GarageRequest;
import swp391.carwash.dto.GarageResponse;
import swp391.carwash.service.GarageService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/garages")
@Tag(name = "Garage Management", description = "APIs for managing garages / branch locations")
public class GarageController {

    private final GarageService garageService;

    @Autowired
    public GarageController(GarageService garageService) {
        this.garageService = garageService;
    }

    @PostMapping
    @Operation(summary = "Create a new garage", description = "Creates a new garage branch. Status defaults to ACTIVE.")
    public ResponseEntity<ApiResponse<GarageResponse>> createGarage(@Valid @RequestBody GarageRequest request) {
        GarageResponse response = garageService.createGarage(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Garage created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get garage by ID", description = "Retrieves details of an active garage by its unique identifier.")
    public ResponseEntity<ApiResponse<GarageResponse>> getGarageById(@PathVariable Integer id) {
        GarageResponse response = garageService.getGarageById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Garage retrieved successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all active garages", description = "Retrieves a list of all active (non-deleted) garages.")
    public ResponseEntity<ApiResponse<List<GarageResponse>>> getAllActiveGarages() {
        List<GarageResponse> responses = garageService.getAllActiveGarages();
        return ResponseEntity.ok(ApiResponse.success(responses, "Active garages retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update garage details", description = "Updates an existing active garage by its ID.")
    public ResponseEntity<ApiResponse<GarageResponse>> updateGarage(
            @PathVariable Integer id,
            @Valid @RequestBody GarageRequest request) {
        GarageResponse response = garageService.updateGarage(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Garage updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete garage", description = "Performs a soft delete on a garage by updating its status to DELETED.")
    public ResponseEntity<ApiResponse<Void>> deleteGarage(@PathVariable Integer id) {
        garageService.softDeleteGarage(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Garage deleted successfully"));
    }
}

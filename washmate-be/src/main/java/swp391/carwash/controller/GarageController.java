package swp391.carwash.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.entity.Garage;
import swp391.carwash.service.GarageService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/garages")
@CrossOrigin(origins = "*") // Tránh lỗi CORS khi gọi từ Front-end (React/Vue/Thymeleaf)
public class GarageController {

    private final GarageService garageService;

    public GarageController(GarageService garageService) {
        this.garageService = garageService;
    }

    // 1. API Tạo mới một Garage
    // POST http://localhost:8080/api/v1/garages
    @PostMapping
    public ResponseEntity<Garage> createGarage(@RequestBody Garage garage) {
        return ResponseEntity.ok(garageService.createGarage(garage));
    }

    // 2. API Lấy danh sách toàn bộ Garage
    // GET http://localhost:8080/api/v1/garages
    @GetMapping
    public ResponseEntity<List<Garage>> getAllGarages() {
        return ResponseEntity.ok(garageService.getAllGarages());
    }

    // 3. API Lấy chi tiết 1 Garage theo ID
    // GET http://localhost:8080/api/v1/garages/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Garage> getGarageById(@PathVariable Long id) {
        return garageService.getGarageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 4. API Cập nhật thông tin Garage
    // PUT http://localhost:8080/api/v1/garages/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Garage> updateGarage(@PathVariable Long id, @RequestBody Garage garageDetails) {
        try {
            return ResponseEntity.ok(garageService.updateGarage(id, garageDetails));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 5. API Xóa một Garage
    // DELETE http://localhost:8080/api/v1/garages/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGarage(@PathVariable Long id) {
        try {
            garageService.deleteGarage(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

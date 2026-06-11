package swp391.carwash.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.entity.Garage;
import swp391.carwash.service.GarageService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/garages")
@CrossOrigin(origins = "*")
@Tag(name = "Garage Management", description = "APIs quản lý danh sách và thông tin các chi nhánh Garage rửa xe")
public class GarageController {

    private final GarageService garageService;

    public GarageController(GarageService garageService) {
        this.garageService = garageService;
    }

    // 1. API Tạo mới một Garage
    // POST http://localhost:8080/api/v1/garages
    @PostMapping
    @Operation(summary = "Tạo mới một Garage", description = "Thêm mới một chi nhánh garage vào hệ thống")
    public ResponseEntity<Garage> createGarage(@RequestBody Garage garage) {
        return ResponseEntity.ok(garageService.createGarage(garage));
    }

    // 2. API Lấy danh sách toàn bộ Garage
    // GET http://localhost:8080/api/v1/garages
    @GetMapping
    @Operation(summary = "Lấy danh sách toàn bộ Garage", description = "Trả về danh sách tất cả các chi nhánh garage hiện có")
    public ResponseEntity<List<Garage>> getAllGarages() {
        return ResponseEntity.ok(garageService.getAllGarages());
    }

    // 3. API Lấy chi tiết 1 Garage theo ID
    // GET http://localhost:8080/api/v1/garages/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết 1 Garage theo ID", description = "Tìm kiếm thông tin chi tiết chi nhánh garage theo ID")
    public ResponseEntity<Garage> getGarageById(@PathVariable Long id) {
        return garageService.getGarageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 4. API Cập nhật thông tin Garage
    // PUT http://localhost:8080/api/v1/garages/{id}
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin Garage", description = "Cập nhật các thông tin như tên, địa chỉ, số điện thoại của garage")
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
    @Operation(summary = "Xóa một Garage", description = "Xóa thông tin chi nhánh garage ra khỏi hệ thống theo ID")
    public ResponseEntity<Void> deleteGarage(@PathVariable Long id) {
        try {
            garageService.deleteGarage(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

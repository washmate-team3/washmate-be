package swp391.carwash.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.request.promotion.PromotionCreateRequest;
import swp391.carwash.dto.request.promotion.PromotionUpdateRequest;
import swp391.carwash.dto.response.PromotionResponse;
import swp391.carwash.service.AdminPromotionService;

@RestController
@RequestMapping("/api/v1/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OWNER','MANAGER')")
public class AdminPromotionController {

    private final AdminPromotionService adminPromotionService;

    @PostMapping
    public ResponseEntity<PromotionResponse> create(
            @Valid @RequestBody PromotionCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(adminPromotionService.create(request));
    }

    @PutMapping("/{promotionId}")
    public ResponseEntity<PromotionResponse> update(
            @PathVariable Integer promotionId,
            @Valid @RequestBody PromotionUpdateRequest request
    ) {
        return ResponseEntity.ok(
                adminPromotionService.update(promotionId, request)
        );
    }

    @GetMapping("/{promotionId}")
    public ResponseEntity<PromotionResponse> getById(
            @PathVariable Integer promotionId
    ) {
        return ResponseEntity.ok(
                adminPromotionService.getById(promotionId)
        );
    }

    @GetMapping
    public ResponseEntity<Page<PromotionResponse>> getAll(
            @RequestParam(required = false) Integer garageId,
            @PageableDefault(
                    size = 10,
                    sort = "id",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                adminPromotionService.getAll(garageId, pageable)
        );
    }

    @DeleteMapping("/{promotionId}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer promotionId
    ) {
        adminPromotionService.delete(promotionId);
        return ResponseEntity.noContent().build();
    }
}
package swp391.carwash.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.dto.request.CampaignSendRequest;
import swp391.carwash.dto.response.CampaignPreviewResponse;
import swp391.carwash.dto.response.CampaignSendResponse;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.service.CustomerCampaignService;

/**
 * Endpoint "insight → hành động" cho Owner/Admin: xem trước và gửi chiến dịch
 * email + voucher tới tệp khách của một insight.
 */
@RestController
@RequiredArgsConstructor
public class CustomerCampaignController {

    private final CustomerCampaignService customerCampaignService;

    @PostMapping("/api/owner/insights/{id}/campaign/preview")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public CampaignPreviewResponse preview(@PathVariable("id") Integer id) {
        return customerCampaignService.preview(id);
    }

    @PostMapping("/api/owner/insights/{id}/campaign/send")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public CampaignSendResponse send(
            @PathVariable("id") Integer id,
            @Valid @RequestBody CampaignSendRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        Integer sentByUserId = principal == null ? null : principal.getId();
        return customerCampaignService.send(id, request, sentByUserId);
    }
}

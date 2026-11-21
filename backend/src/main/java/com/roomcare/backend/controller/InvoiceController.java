package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.invoice.AdjustInvoiceRequest;
import com.roomcare.backend.dto.invoice.BatchCreateInvoiceRequest;
import com.roomcare.backend.dto.invoice.BatchCreateInvoiceResult;
import com.roomcare.backend.dto.invoice.CancelInvoiceRequest;
import com.roomcare.backend.dto.invoice.CreateInvoiceRequest;
import com.roomcare.backend.dto.invoice.CreatePaymentRequest;
import com.roomcare.backend.dto.invoice.InvoiceResponse;
import com.roomcare.backend.entity.InvoiceStatus;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/landlord/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ApiResponse<List<InvoiceResponse>> getInvoices(
            @AuthenticationPrincipal User landlord,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) InvoiceStatus status) {
        List<InvoiceResponse> responses = invoiceService.getInvoices(
                landlord.getId(), month, roomId, propertyId, status
        );
        return ApiResponse.success(responses);
    }

    @GetMapping("/{id}")
    public ApiResponse<InvoiceResponse> getInvoiceById(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id) {
        InvoiceResponse response = invoiceService.getInvoiceById(landlord.getId(), id);
        return ApiResponse.success(response);
    }

    @PostMapping("/preview")
    public ApiResponse<InvoiceResponse> previewInvoice(
            @AuthenticationPrincipal User landlord,
            @Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = invoiceService.previewInvoice(landlord.getId(), request);
        return ApiResponse.success("Xem trước hóa đơn thành công", response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvoiceResponse> createInvoice(
            @AuthenticationPrincipal User landlord,
            @Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(landlord.getId(), request);
        String msg = Boolean.TRUE.equals(request.getSaveAsDraft())
                ? "Lưu hóa đơn nháp thành công"
                : "Tạo hóa đơn thành công";
        return ApiResponse.success(msg, response);
    }

    @PutMapping("/{id}")
    public ApiResponse<InvoiceResponse> updateDraftInvoice(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id,
            @Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = invoiceService.updateDraftInvoice(landlord.getId(), id, request);
        return ApiResponse.success("Cập nhật hóa đơn nháp thành công", response);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<InvoiceResponse> publishInvoice(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id) {
        InvoiceResponse response = invoiceService.publishInvoice(landlord.getId(), id);
        return ApiResponse.success("Phát hành hóa đơn thành công", response);
    }

    @PostMapping("/{id}/adjust")
    public ApiResponse<InvoiceResponse> adjustInvoice(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id,
            @Valid @RequestBody AdjustInvoiceRequest request) {
        InvoiceResponse response = invoiceService.adjustInvoice(landlord.getId(), id, request);
        return ApiResponse.success("Điều chỉnh hóa đơn thành công", response);
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<InvoiceResponse> cancelInvoice(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelInvoiceRequest request) {
        InvoiceResponse response = invoiceService.cancelInvoice(landlord.getId(), id, request);
        return ApiResponse.success("Hủy hóa đơn thành công", response);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BatchCreateInvoiceResult> batchCreateInvoices(
            @AuthenticationPrincipal User landlord,
            @Valid @RequestBody BatchCreateInvoiceRequest request) {
        BatchCreateInvoiceResult result = invoiceService.batchCreateInvoices(landlord.getId(), request);
        return ApiResponse.success(
                "Đã tạo " + result.getCreatedCount() + " hóa đơn, bỏ qua " + result.getSkippedCount() + " phòng",
                result
        );
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvoiceResponse> recordPayment(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id,
            @Valid @RequestBody CreatePaymentRequest request) {
        InvoiceResponse response = invoiceService.recordPayment(landlord.getId(), id, request);
        return ApiResponse.success("Ghi nhận thanh toán thành công", response);
    }
}

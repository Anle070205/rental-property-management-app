package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.invoice.BatchCreateInvoiceRequest;
import com.roomcare.backend.dto.invoice.BatchCreateInvoiceResult;
import com.roomcare.backend.dto.invoice.CreateInvoiceRequest;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvoiceResponse> createInvoice(
            @AuthenticationPrincipal User landlord,
            @Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(landlord.getId(), request);
        return ApiResponse.success("Tạo hóa đơn thành công", response);
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
}

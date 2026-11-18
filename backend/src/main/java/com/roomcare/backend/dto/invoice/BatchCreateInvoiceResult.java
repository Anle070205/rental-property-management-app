package com.roomcare.backend.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchCreateInvoiceResult {
    private int createdCount;
    private int skippedCount;
    private List<InvoiceResponse> createdInvoices = new ArrayList<>();
    private List<String> skippedReasons = new ArrayList<>();
}

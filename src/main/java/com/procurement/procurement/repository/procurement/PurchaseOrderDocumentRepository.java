package com.procurement.procurement.repository.procurement;

import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.procurement.PurchaseOrderDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderDocumentRepository extends JpaRepository<PurchaseOrderDocument, Long> {
    List<PurchaseOrderDocument> findByPurchaseOrder(PurchaseOrder purchaseOrder);

    List<PurchaseOrderDocument> findByPurchaseOrderId(Long purchaseOrderId);
}
